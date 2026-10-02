package com.pgs.device.service.repository;

import org.springframework.data.jpa.domain.Specification;

import com.pgs.device.service.entity.Device;
import com.pgs.device.service.model.DeviceType;

/**
 * Specifications for filtering {@link Device} queries.
 */
public final class DeviceSpecification {

    private DeviceSpecification() {
    }

    /**
     * Creates a specification that filters devices by name containing the given value,
     * ignoring case.
     *
     * @param name the name value to search for
     * @return the name filter specification, or no predicate if the value is blank
     */
    public static Specification<Device> nameContains(String name) {
        return (root, query, cb) -> isBlank(name)
                ? null
                : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    /**
     * Creates a specification that filters devices by type.
     *
     * @param type the device type to match
     * @return the type filter specification, or no predicate if the type is null
     */
    public static Specification<Device> typeEquals(DeviceType type) {
        return (root, query, cb) -> type == null
                ? null
                : cb.equal(root.get("type"), type);
    }

    /**
     * Creates a specification that filters devices by location containing the given value,
     * ignoring case.
     *
     * @param location the location value to search for
     * @return the location filter specification, or no predicate if the value is blank
     */
    public static Specification<Device> locationContains(String location) {
        return (root, query, cb) -> isBlank(location)
                ? null
                : cb.like(cb.lower(root.get("location")), "%" + location.toLowerCase() + "%");
    }

    /**
     * Creates a specification that filters devices by user ID.
     *
     * @param userId the user ID to match
     * @return the user ID filter specification, or no predicate if the user ID is null
     */
    public static Specification<Device> userIdEquals(Long userId) {
        return (root, query, cb) -> userId == null
                ? null
                : cb.equal(root.get("userId"), userId);
    }

    /**
     * Checks whether the given value is null or contains only whitespace.
     *
     * @param value the value to check
     * @return {@code true} if the value is null or blank
     */
    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

