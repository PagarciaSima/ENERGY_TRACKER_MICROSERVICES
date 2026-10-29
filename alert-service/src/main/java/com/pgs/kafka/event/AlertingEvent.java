package com.pgs.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain event published to the {@code energy-alerts} Kafka topic when a user
 * exceeds their configured energy consumption threshold.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertingEvent {

    /** The id of the user whose threshold was exceeded. */
    private Long userId;

    /** A human-readable description of the alert. */
    private String message;

    /** The configured consumption threshold. */
    private double threshold;

    /** The aggregated consumption that triggered the alert. */
    private double energyConsumed;

    /** The email address to notify. */
    private String email;
}