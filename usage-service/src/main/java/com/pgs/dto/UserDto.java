package com.pgs.dto;

import lombok.Builder;

/**
 * Data Transfer Object representing a user.
 * <p>
 * This record encapsulates user information including personal details and alert settings.
 * It is immutable and can be built using the {@code @Builder} annotation provided by Lombok.
 *
 * @param id                     Unique identifier of the user.
 * @param name                   First name of the user.
 * @param surname                Last name (surname) of the user.
 * @param email                  Email address of the user.
 * @param address                Physical address of the user.
 * @param alerting               Flag indicating whether alerting is enabled for the user.
 * @param energyAlertingThreshold Threshold value for energy alerts. Alerts are triggered when energy consumption exceeds this value.
 */
@Builder
public record UserDto(
        Long id,
        String name,
        String surname,
        String email,
        String address,
        boolean alerting,
        double energyAlertingThreshold
) {
}