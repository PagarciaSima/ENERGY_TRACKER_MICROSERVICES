package com.pgs.alert.service.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.pgs.alert.service.entity.Alert;
import com.pgs.alert.service.repository.AlertRepository;
import com.pgs.kafka.event.AlertingEvent;

/**
 * End-to-end test that boots the full application against an embedded Kafka
 * broker.
 * <p>
 * {@code @EmbeddedKafka} registers the broker addresses in the
 * {@code spring.kafka.bootstrap-servers} property, so the real listener
 * container (configured in {@code KafkaConfig}) consumes from it. The
 * {@link JavaMailSender} and {@link AlertRepository} are replaced by mocks so
 * no SMTP server or database is required. A real {@code AlertingEvent} is
 * published, and the test verifies the listener -&gt; email -&gt; persistence chain.
 */
@SpringBootTest(properties = "spring.kafka.listener.auto-startup=true")
@EmbeddedKafka(partitions = 1, topics = AlertKafkaIntegrationTest.TOPIC)
@DirtiesContext
class AlertKafkaIntegrationTest {

    static final String TOPIC = "energy-alerts";

    @MockitoBean
    private JavaMailSender mailSender;

    @MockitoBean
    private AlertRepository alertRepository;

    @Value("${spring.embedded.kafka.brokers}")
    private String brokerAddresses;

    private KafkaTemplate<String, AlertingEvent> producer;

    /* ------------------------------------------------------------------ */
    /*  Producer lifecycle (points at the embedded broker)                 */
    /* ------------------------------------------------------------------ */

    @BeforeEach
    void setUpProducer() {
        final Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerAddresses);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);

        final ProducerFactory<String, AlertingEvent> factory =
                new DefaultKafkaProducerFactory<>(props);
        producer = new KafkaTemplate<>(factory);
    }

    @AfterEach
    void closeProducer() {
        if (producer != null) {
            producer.destroy();
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Tests                                                              */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("publishing an AlertingEvent triggers the email and persists a sent=true alert")
    void consumesEventAndSendsEmailAndPersistsAudit() throws Exception {
        final CountDownLatch mailLatch = new CountDownLatch(1);
        doAnswer(invocation -> {
            mailLatch.countDown();
            return null;
        }).when(mailSender).send(any(SimpleMailMessage.class));

        final AlertingEvent event = AlertingEvent.builder()
                .userId(1L)
                .message("Energy consumption threshold exceeded")
                .threshold(100.0)
                .energyConsumed(150.0)
                .email("ana.garcia@example.com")
                .build();

        producer.send(TOPIC, event).get(5, TimeUnit.SECONDS);

        assertThat(mailLatch.await(10, TimeUnit.SECONDS))
                .as("email should be sent once the event is consumed")
                .isTrue();

        final ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).saveAndFlush(captor.capture());

        final Alert saved = captor.getValue();
        assertThat(saved.isSent()).isTrue();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getMessage()).isEqualTo("Energy consumption threshold exceeded");
        assertThat(saved.getThreshold()).isEqualTo(100.0);
        assertThat(saved.getEnergyConsumed()).isEqualTo(150.0);
        assertThat(saved.getEmail()).isEqualTo("ana.garcia@example.com");
    }
}