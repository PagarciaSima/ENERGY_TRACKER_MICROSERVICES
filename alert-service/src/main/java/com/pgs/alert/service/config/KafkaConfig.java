package com.pgs.alert.service.config;

import java.util.function.BiFunction;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka configuration for the Alert Service.
 * <p>
 * It declares a single {@link CommonErrorHandler} bean, which Spring Boot wires
 * into the default {@code kafkaListenerContainerFactory}. The handler retries
 * each failed record a limited number of times and, once the attempts are
 * exhausted, republishes the record to the dead-letter topic via a
 * {@link DeadLetterPublishingRecoverer}. This guarantees that undeliverable
 * alert events are not dropped silently and can be inspected / replayed later
 * (e.g. from the Kafka UI).
 */
@Configuration
public class KafkaConfig {

    private static final long RETRY_BACKOFF_MS = 5000L;
    private static final long MAX_ATTEMPTS = 3L;
    private static final String DLT_SUFFIX = "-dlt";

    private final String alertTopic;
    private final String alertDltTopic;

    public KafkaConfig(
            @Value("${app.kafka.alerts.topic}") String alertTopic,
            @Value("${app.kafka.alerts.dlt-topic}") String alertDltTopic) {
        this.alertTopic = alertTopic;
        this.alertDltTopic = alertDltTopic;
    }

    /**
     * Builds the error handler applied to all {@code @KafkaListener} containers:
     * failed records are retried up to 3 times with a fixed 5s back-off and then
     * routed to the dead-letter topic.
     *
     * @param deadLetterTemplate the producer template used to republish failed records
     * @return the retryable / dead-letter aware error handler
     */
    @Bean
    public CommonErrorHandler commonErrorHandler(KafkaTemplate<?, ?> deadLetterTemplate) {
        final FixedBackOff backOff = new FixedBackOff(RETRY_BACKOFF_MS, MAX_ATTEMPTS - 1L);
        return new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(deadLetterTemplate, destinationResolver()),
                backOff);
    }

    /**
     * Resolves the destination of a dead-lettered record. The alerts topic is
     * mapped to its explicitly configured {@code -dlt} counterpart; any other
     * topic falls back to the default {@code <topic>-dlt} convention.
     *
     * @return the function mapping a failed record to its dead-letter topic-partition
     */
    private BiFunction<ConsumerRecord<?, ?>, Exception, TopicPartition> destinationResolver() {
        return (record, exception) -> {
            final String originalTopic = record.topic();
            final String dltTopic = originalTopic.equals(alertTopic) ? alertDltTopic : originalTopic + DLT_SUFFIX;
            return new TopicPartition(dltTopic, record.partition());
        };
    }
}