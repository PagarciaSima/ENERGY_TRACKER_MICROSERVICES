package com.pgs.usage.service.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Data Transfer Object representing aggregated energy usage for a user over a period.
 * <p>
 * This record encapsulates the user identifier and a list of devices along with their
 * aggregated consumption values. It is immutable and can be constructed using the
 * {@code @Builder} annotation provided by Lombok.
 *
 * @param userId  Unique identifier of the user.
 * @param devices List of devices with their aggregated consumption.
 */
@Builder
@Schema(description = "Aggregated energy usage for a user over a period")
public record UsageDto(
        @Schema(description = "User id", example = "1")
        Long userId,

        @Schema(description = "Devices with their aggregated consumption")
        List<DeviceDto> devices) {}