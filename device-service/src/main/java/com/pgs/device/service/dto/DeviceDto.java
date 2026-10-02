package com.pgs.device.service.dto;

import com.pgs.device.service.model.DeviceType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Device data transfer object")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceDto {

    @Schema(description = "Device id (auto-generated)", example = "1")
    private Long id;

    @NotBlank(message = "name must not be blank")
    @Size(max = 100, message = "name must be at most 100 characters")
    @Schema(description = "Device name", example = "Salón", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull(message = "type must not be null")
    @Schema(description = "Device type", example = "LIGHT", requiredMode = Schema.RequiredMode.REQUIRED)
    private DeviceType type;

    @Size(max = 255, message = "location must be at most 255 characters")
    @Schema(description = "Device location", example = "Salón principal")
    private String location;

    @NotNull(message = "userId must not be null")
    @Schema(description = "Owner user id", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;
}