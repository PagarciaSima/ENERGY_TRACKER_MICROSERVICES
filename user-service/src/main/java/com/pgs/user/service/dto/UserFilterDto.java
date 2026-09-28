package com.pgs.user.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Filter criteria for searching users.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFilterDto {

    /** Optional name fragment. */
    private String name;

    /** Optional surname fragment. */
    private String surname;

    /** Optional email fragment. */
    private String email;

    /** Optional address fragment. */
    private String address;

    /** Optional alerting flag. */
    private Boolean alerting;

    /** Optional minimum energy alerting threshold. */
    private Double minEnergyAlertingThreshold;
}
