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
     * For each alert, it builds a human-readable subject and body, and delegates
     * the actual delivery to {@link EmailService#sendEmail(String, String, String, Long)}.
     *
     * @param alertingEvent the alert event containing the user id, the email
     *                      address, the threshold and the aggregated consumption
     *                      that triggered the alert
     */
    @KafkaListener(topics = "energy-alerts", groupId = "alert-service")
    public void energyUsageAlertEvent(AlertingEvent alertingEvent) {
        log.info("Received alert event: {}", alertingEvent);

        // send email alert
        final String subject = "Energy Usage Alert for User "
                + alertingEvent.getUserId();

        final String message = "Alert: " + alertingEvent.getMessage() +
                "\nThreshold: " + alertingEvent.getThreshold() +
                "\nEnergy Consumed: " + alertingEvent.getEnergyConsumed();

        emailService.sendEmail(alertingEvent.getEmail(),
                subject,
                message,
                alertingEvent.getUserId());
    }
}