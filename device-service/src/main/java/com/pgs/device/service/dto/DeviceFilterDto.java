package com.pgs.device.service.dto;

import com.pgs.device.service.model.DeviceType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Optional filters for searching devices.
 */
@Schema(description = "Optional filters for searching devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceFilterDto {

    @Schema(description = "Name fragment (case-insensitive)", example = "salón")
    private String name;

    @Schema(description = "Exact device type", example = "LIGHT")
    private DeviceType type;

    @Schema(description = "Location fragment (case-insensitive)", example = "madrid")
    private String location;

    @Schema(description = "Owner user id", example = "1")
    private Long userId;
}