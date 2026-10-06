package com.pgs.ingestion.service.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;

/**
 * Energy usage reading received from an IoT device.
 * <p>
 * Each reading represents the amount of energy consumed by a device
 * over a period of time, at a specific moment (timestamp).
 *
 * @param deviceId        the id of the device that reported the reading
 * @param energyConsumed  the energy consumed (in kWh) since the last reading
 * @param timestamp       the moment when the reading was taken
 */
@Builder
public record EnergyUsageDto(

        @NotNull(message = "deviceId must not be null")
        Long deviceId,

        @PositiveOrZero(message = "energyConsumed must be zero or positive")
        double energyConsumed,

        @NotNull(message = "timestamp must not be null")
        @PastOrPresent(message = "timestamp must not be in the future")
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant timestamp) {}