package com.pgs.user.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Data Transfer Object representing a user.
 */
@Schema(
        description = "Data Transfer Object representing a user",
        example = "{\"id\": 1, \"name\": \"Ana\", \"surname\": \"García\", \"email\": \"ana.garcia@example.com\", \"address\": \"Calle Mayor 5, Madrid\", \"alerting\": true, \"energyAlertingThreshold\": 3200.5}")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    /** Unique identifier of the user. */
    @Schema(description = "Unique identifier of the user (auto-generated)", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    /** First name of the user. */
    @Schema(description = "First name of the user", example = "Ana", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    /** Last name of the user. */
    @Schema(description = "Last name of the user", example = "García", requiredMode = Schema.RequiredMode.REQUIRED)
    private String surname;

    /** Email address of the user. */
    @ToString.Exclude
    @Schema(description = "Email address of the user", example = "ana.garcia@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    /** Physical address of the user. */
    @Schema(description = "Physical address of the user", example = "Calle Mayor 5, Madrid", requiredMode = Schema.RequiredMode.REQUIRED)
    private String address;

    /** Indicates whether alerting is enabled for the user. */
    @Schema(description = "Whether energy alerting is enabled for the user", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean alerting;

    /** Threshold value that triggers an energy alert. */
    @Schema(description = "Energy alerting threshold of the user", example = "3200.5", requiredMode = Schema.RequiredMode.REQUIRED)
    private double energyAlertingThreshold;

}
