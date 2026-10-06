package com.pgs.usage.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Data Transfer Object representing device information returned by the device-service.
 * <p>
 * This record encapsulates details about a device, including its identifier, name, type,
 * location, owner, and energy consumption. It is immutable and can be constructed using the
 * {@code @Builder} annotation provided by Lombok.
 *
 * @param id             Unique identifier of the device.
 * @param name           Name of the device.
 * @param type           Type of the device (e.g., LIGHT).
 * @param location       Physical location of the device.
 * @param userId         Identifier of the user who owns the device.
 * @param energyConsumed Energy consumed by the device in kWh.
 */
@Builder
@Schema(description = "Device information returned by the device-service")
public record DeviceDto(
        @Schema(description = "Device id", example = "42")
        Long id,

        @Schema(description = "Device name", example = "Smart Bulb Salón")
        String name,

        @Schema(description = "Device type", example = "LIGHT")
        String type,

        @Schema(description = "Device location", example = "Salón principal")
        String location,

        @Schema(description = "Owner user id", example = "1")
        Long userId,

        @Schema(description = "Energy consumed by the device (kWh)", example = "3.5")
        Double energyConsumed) {}