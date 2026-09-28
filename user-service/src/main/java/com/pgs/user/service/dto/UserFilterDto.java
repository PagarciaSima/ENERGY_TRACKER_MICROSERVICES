package com.pgs.user.service.dto;

import org.springdoc.core.annotations.ParameterObject;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Filter criteria for searching users.
 */
@ParameterObject
@Schema(description = "Filter criteria for searching users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFilterDto {

    /** Optional name fragment. */
    @Schema(description = "Optional name fragment", example = "Ana")
    private String name;

    /** Optional surname fragment. */
    @Schema(description = "Optional surname fragment", example = "García")
    private String surname;

    /** Optional email fragment. */
    @Schema(description = "Optional email fragment", example = "garcia@")
    private String email;

    /** Optional address fragment. */
    @Schema(description = "Optional address fragment", example = "Madrid")
    private String address;

    /** Optional alerting flag. */
    @Schema(description = "Optional alerting flag", example = "true")
    private Boolean alerting;

    /** Optional minimum energy alerting threshold. */
    @Schema(description = "Optional minimum energy alerting threshold", example = "2500.0")
    private Double minEnergyAlertingThreshold;
}
