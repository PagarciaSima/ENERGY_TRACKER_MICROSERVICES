package com.pgs.device.service.entity;

import com.pgs.device.service.model.DeviceType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA entity representing a device associated with a user.
 */
@Entity
@Table(name = "device")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class Device {

    /**
     * Unique identifier of the device.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Name assigned to the device.
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Type of device.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private DeviceType type;

    /**
     * Physical location of the device.
     */
    @Column(nullable = false, length = 255)
    private String location;

    /**
     * Identifier of the user who owns the device.
     */
    @Column(nullable = false)
    private Long userId;
}

