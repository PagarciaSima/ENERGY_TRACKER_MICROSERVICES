package com.pgs.alert.service.service;

import java.time.Instant;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.pgs.alert.service.entity.Alert;
import com.pgs.alert.service.repository.AlertRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for sending alert emails and recording the outcome.
 * <p>
 * Each call to {@link #sendEmail(String, String, String, Long)} attempts to
 * deliver an email through the configured {@link JavaMailSender} and, regardless
 * of the result, persists an {@link Alert} record so that the notification
 * history can be audited later.
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
     * Sends an email to the given recipient and records the outcome.
     * <p>
     * If the email is delivered successfully, an {@link Alert} with
     * {@code sent = true} is stored. If the send fails with a {@link MailException},
     * an {@link Alert} with {@code sent = false} is stored instead, and the
     * exception is swallowed so that the caller (typically a Kafka listener)
     * is not forced to handle it.
     *
     * @param to      the recipient email address; must not be {@code null}
     * @param subject the email subject
     * @param body    the email body
     * @param userId  the id of the user the email is addressed to; used to
     *                link the {@link Alert} record with the user
     */
    public void sendEmail(String to, String subject, String body, Long userId) {
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

        saveAlert(userId, sent);
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
     * Persists an {@link Alert} record indicating whether the email was sent.
     *
     * @param userId the id of the user the email was addressed to
     * @param sent   {@code true} if the email was sent successfully, {@code false} otherwise
     */
    private void saveAlert(Long userId, boolean sent) {
        final Alert alert = Alert.builder()
                .sent(sent)
                .createdAt(Instant.now())
                .userId(userId)
                .build();
        alertRepository.saveAndFlush(alert);
    }
}