package com.pgs.kafka.event;

import lombok.Builder;

/**
 * Domain event published to the {@code energy-alerts} Kafka topic when a user
 * exceeds their configured energy consumption threshold.
 *
 * @param userId         the id of the user whose threshold was exceeded
 * @param message        a human-readable description of the alert
 * @param threshold      the configured consumption threshold
 * @param energyConsumed the aggregated consumption that triggered the alert
 * @param email          the email address to notify
 */
@Builder
public record AlertingEvent(
        Long userId,
        String message,
        double threshold,
        double energyConsumed,
        String email
) {
}