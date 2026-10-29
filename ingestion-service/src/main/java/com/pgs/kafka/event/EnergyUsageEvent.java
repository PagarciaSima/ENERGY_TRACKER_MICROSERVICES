package com.pgs.kafka.event;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Builder;

/**
 * Domain event published to the {@code energy-usage} Kafka topic for every
 * energy usage reading ingested from an IoT device.
 *
 * @param deviceId       the id of the device that produced the reading
 * @param energyConsumed the energy consumed by the device, in kWh
 * @param timestamp      the instant at which the reading was taken
 */
@Builder
public record EnergyUsageEvent (
        Long deviceId,
        double energyConsumed,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant timestamp) {}