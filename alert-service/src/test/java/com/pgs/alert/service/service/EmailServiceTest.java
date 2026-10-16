package com.pgs.alert.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.pgs.alert.service.entity.Alert;
import com.pgs.alert.service.repository.AlertRepository;
import com.pgs.kafka.event.AlertingEvent;

/**
 * Unit tests for {@link EmailService}. The {@link JavaMailSender} and the
 * {@link AlertRepository} are mocked so no SMTP server or database is required.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private AlertRepository alertRepository;

    @InjectMocks
    private EmailService emailService;

    /* ------------------------------------------------------------------ */
    /*  Test data helpers                                                  */
    /* ------------------------------------------------------------------ */

    private static AlertingEvent sampleEvent() {
        return AlertingEvent.builder()
                .userId(1L)
                .message("Energy consumption threshold exceeded")
                .threshold(100.0)
                .energyConsumed(150.0)
                .email("ana.garcia@example.com")
                .build();
    }

    /* ------------------------------------------------------------------ */
    /*  sendEmail                                                          */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("sendEmail")
    class SendEmail {

        @Test
        @DisplayName("sends the email and persists a sent=true alert with full audit content")
        void sendsEmailAndPersistsSentAlert() {
            emailService.sendEmail(sampleEvent());

            verify(mailSender).send(any(SimpleMailMessage.class));

            final ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
            verify(alertRepository).saveAndFlush(captor.capture());

            final Alert saved = captor.getValue();
            assertThat(saved.isSent()).isTrue();
            assertThat(saved.getUserId()).isEqualTo(1L);
            assertThat(saved.getMessage()).isEqualTo("Energy consumption threshold exceeded");
            assertThat(saved.getThreshold()).isEqualTo(100.0);
            assertThat(saved.getEnergyConsumed()).isEqualTo(150.0);
            assertThat(saved.getEmail()).isEqualTo("ana.garcia@example.com");
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("persists a sent=false alert when the SMTP send fails")
        void persistsAlertWithSentFalseWhenMailFails() {
            doThrow(new MailSendException("smtp down"))
                    .when(mailSender).send(any(SimpleMailMessage.class));

            emailService.sendEmail(sampleEvent());

            final ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
            verify(alertRepository).saveAndFlush(captor.capture());

            assertThat(captor.getValue().isSent()).isFalse();
            assertThat(captor.getValue().getUserId()).isEqualTo(1L);
        }
    }
}