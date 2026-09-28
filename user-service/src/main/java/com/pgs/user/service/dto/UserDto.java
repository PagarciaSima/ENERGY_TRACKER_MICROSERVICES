package com.pgs.user.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Data Transfer Object representing a user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    /** Unique identifier of the user. */
    private Long id;

    /** First name of the user. */
    private String name;

    /** Last name of the user. */
    private String surname;

    /** Email address of the user. */
    @ToString.Exclude
    private String email;

    /** Physical address of the user. */
    private String address;

    /** Indicates whether alerting is enabled for the user. */
    private boolean alerting;

    /** Threshold value that triggers an energy alert. */
    private double energyAlertingThreshold;

}