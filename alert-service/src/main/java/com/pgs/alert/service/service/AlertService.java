package com.pgs.alert.service.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.pgs.kafka.event.AlertingEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for handling energy usage alerts.
 * <p>
 * It consumes {@link AlertingEvent} messages from the {@code energy-alerts} Kafka
 * topic and delegates the actual notification to the {@link EmailService}.
 * <p>
 * This service is intentionally thin: it does not decide <em>when</em> an alert
 * must be raised (that is the responsibility of the {@code usage-service}),
 * it only reacts to alert events and forwards them to the notification channel.
 * This separation allows the notification channel (email, push, SMS, etc.) to
 * evolve independently from the alert detection logic.
 * <p>
 * The consumer factory is customized in {@code KafkaConfig} with a retryable
 * error handler that, after a configured number of attempts, delegates to a
 * dead-letter topic so that undeliverable events are not silently lost.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final EmailService emailService;

    /**
     * Kafka listener that consumes {@link AlertingEvent} messages from the
     * {@code energy-alerts} topic.
     * <p>
     * Events missing mandatory fields (user id, email) are ignored with a warning;
     * well-formed events are forwarded to {@link EmailService#sendEmail(AlertingEvent)}
     * which builds and sends the notification.
     *
     * @param alertingEvent the alert event containing the user id, the email
     *                      address, the threshold and the aggregated consumption
     *                      that triggered the alert
     */
    @KafkaListener(topics = "${app.kafka.alerts.topic}",
            groupId = "${spring.kafka.consumer.group-id:alert-service}")
    public void energyUsageAlertEvent(AlertingEvent alertingEvent) {
        log.info("Received alert event: {}", alertingEvent);

        if (alertingEvent == null
                || alertingEvent.getUserId() == null
                || alertingEvent.getEmail() == null) {
            log.warn("Ignoring alert event with missing mandatory fields");
            return;
        }

        emailService.sendEmail(alertingEvent);
    }
}