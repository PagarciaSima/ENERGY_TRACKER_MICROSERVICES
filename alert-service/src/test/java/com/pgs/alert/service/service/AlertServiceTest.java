package com.pgs.alert.service.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pgs.kafka.event.AlertingEvent;

/**
 * Unit tests for {@link AlertService}. The {@link EmailService} is mocked so
 * the listener logic (delegation and null-guards) is verified in isolation.
 */
@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AlertService alertService;

    /* ------------------------------------------------------------------ */
    /*  Test data helpers                                                  */
    /* ------------------------------------------------------------------ */

    private static AlertingEvent validEvent() {
        return AlertingEvent.builder()
                .userId(1L)
                .message("Energy consumption threshold exceeded")
                .threshold(100.0)
                .energyConsumed(150.0)
                .email("ana.garcia@example.com")
                .build();
    }

    /* ------------------------------------------------------------------ */
    /*  energyUsageAlertEvent                                              */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("energyUsageAlertEvent")
    class EnergyUsageAlertEvent {

        @Test
        void delegatesValidEventToEmailService() {
            final AlertingEvent event = validEvent();

            alertService.energyUsageAlertEvent(event);

            verify(emailService).sendEmail(event);
        }

        @Test
        void ignoresNullEvent() {
            alertService.energyUsageAlertEvent(null);

            verify(emailService, never()).sendEmail(ArgumentMatchers.any());
        }

        @Test
        void ignoresEventWithoutUserId() {
            final AlertingEvent event = validEvent();
            event.setUserId(null);

            alertService.energyUsageAlertEvent(event);

            verify(emailService, never()).sendEmail(ArgumentMatchers.any());
        }

        @Test
        void ignoresEventWithoutEmail() {
            final AlertingEvent event = validEvent();
            event.setEmail(null);

            alertService.energyUsageAlertEvent(event);

            verify(emailService, never()).sendEmail(ArgumentMatchers.any());
        }
    }
}