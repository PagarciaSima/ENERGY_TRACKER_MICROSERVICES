package com.pgs.alert.service.service;

import java.time.Instant;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.pgs.alert.service.entity.Alert;
import com.pgs.alert.service.repository.AlertRepository;
import com.pgs.kafka.event.AlertingEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for sending alert emails and recording the outcome.
 * <p>
 * Each {@link AlertingEvent} is turned into a plain-text email (subject + body)
 * and delivered through the configured {@link JavaMailSender}. Regardless of the
 * delivery result, an {@link Alert} record is persisted so that the notification
 * history can be audited later, including the exact content (message, threshold,
 * energy consumed, recipient) that triggered the alert.
 * <p>
 * The email is sent synchronously: if the SMTP server is slow, the calling
 * Kafka listener will block until the send completes or fails. For this
 * project the volume is low enough that this trade-off is acceptable.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String FROM_ADDRESS = "noreply@pgs.com";

    private final JavaMailSender mailSender;
    private final AlertRepository alertRepository;

    /**
     * Sends an email derived from the given alert event and records the outcome.
     * <p>
     * If the email is delivered successfully, an {@link Alert} with
     * {@code sent = true} is stored. If the send fails with a {@link MailException},
     * an {@link Alert} with {@code sent = false} is stored instead, and the
     * exception is swallowed so that the caller (typically a Kafka listener)
     * is not forced to handle it.
     *
     * @param event the alert event; must have a non-{@code null} userId and email
     */
    public void sendEmail(AlertingEvent event) {
        final String to = event.getEmail();
        final String subject = buildSubject(event);
        final String body = buildBody(event);

        log.info("Sending email to: {}, subject: {}", to, subject);

        final SimpleMailMessage message = buildMessage(to, subject, body);

        boolean sent;
        try {
            mailSender.send(message);
            sent = true;
            log.info("Email sent to: {}", to);
        } catch (MailException e) {
            sent = false;
            log.error("Failed to send email to: {}", to, e);
        }

        saveAlert(event, sent);
    }

    /**
     * Builds the email subject from the event data.
     *
     * @param event the alert event
     * @return the subject line
     */
    private String buildSubject(AlertingEvent event) {
        return "Energy Usage Alert for User " + event.getUserId();
    }

    /**
     * Builds the plain-text email body from the event data.
     *
     * @param event the alert event
     * @return the body text
     */
    private String buildBody(AlertingEvent event) {
        return "Alert: " + event.getMessage() +
                "\nThreshold: " + event.getThreshold() +
                "\nEnergy Consumed: " + event.getEnergyConsumed();
    }

    /**
     * Builds a {@link SimpleMailMessage} with the sender, recipient, subject and body.
     *
     * @param to      the recipient email address
     * @param subject the email subject
     * @param body    the email body
     * @return the message ready to be sent
     */
    private SimpleMailMessage buildMessage(String to, String subject, String body) {
        final SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(FROM_ADDRESS);
        message.setSubject(subject);
        message.setText(body);
        return message;
    }

    /**
     * Persists an {@link Alert} with the full audit content of the notification.
     *
     * @param event the alert event that produced the notification
     * @param sent  {@code true} if the email was sent successfully, {@code false} otherwise
     */
    private void saveAlert(AlertingEvent event, boolean sent) {
        final Alert alert = Alert.builder()
                .sent(sent)
                .createdAt(Instant.now())
                .userId(event.getUserId())
                .message(event.getMessage())
                .threshold(event.getThreshold())
                .energyConsumed(event.getEnergyConsumed())
                .email(event.getEmail())
                .build();
        alertRepository.saveAndFlush(alert);
    }
}