package com.pgs.usage.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model that pairs a device id with its aggregated energy consumption.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceEnergy {

    /** The device id. */
    private Long deviceId;

    /** The aggregated energy consumed by the device. */
    private double energyConsumed;

    /** The id of the user that owns the device, resolved from the Device Service. */
    private Long userId;
}
