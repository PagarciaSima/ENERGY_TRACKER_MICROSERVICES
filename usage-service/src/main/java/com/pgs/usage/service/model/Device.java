package com.pgs.usage.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * In-memory model of a device enriched with its aggregated energy consumption.
 * <p>
 * It is used when combining device data retrieved from the Device Service with
 * the consumption aggregated from InfluxDB.
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Device {

    /** The device id. */
    Long id;

    /** The device name. */
    String name;

    /** The device type. */
    String type;

    /** The device location. */
    String location;

    /** The id of the user that owns the device. */
    Long userId;

    /** The aggregated energy consumed by the device. */
    Double energyConsumed;
}
