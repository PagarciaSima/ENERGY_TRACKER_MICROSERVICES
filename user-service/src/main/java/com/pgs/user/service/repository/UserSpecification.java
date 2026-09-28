package com.pgs.user.service.repository;

import org.springframework.data.jpa.domain.Specification;

import com.pgs.user.service.entity.User;

/**
 * Specifications for filtering {@link User} queries.
 */
public final class UserSpecification {

    private UserSpecification() {
    }

    /**
     * Filters users whose name contains the given value (case-insensitive).
     *
     * @param name the name fragment
     * @return the specification, or {@code null} if the name is null/blank
     */
    public static Specification<User> nameContains(String name) {
        return (root, query, cb) -> isBlank(name)
                ? null
                : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    /**
     * Filters users whose surname contains the given value (case-insensitive).
     *
     * @param surname the surname fragment
     * @return the specification, or {@code null} if the surname is null/blank
     */
    public static Specification<User> surnameContains(String surname) {
        return (root, query, cb) -> isBlank(surname)
                ? null
                : cb.like(cb.lower(root.get("surname")), "%" + surname.toLowerCase() + "%");
    }

    /**
     * Filters users whose email contains the given value (case-insensitive).
     *
     * @param email the email fragment
     * @return the specification, or {@code null} if the email is null/blank
     */
    public static Specification<User> emailContains(String email) {
        return (root, query, cb) -> isBlank(email)
                ? null
                : cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
    }
    
    /**
     * Filters users whose address contains the given value (case-insensitive).
     *
     * @param address the address fragment
     * @return the specification, or {@code null} if the address is null/blank
     */
    public static Specification<User> addressContains(String address) {
        return (root, query, cb) -> isBlank(address)
                ? null
                : cb.like(cb.lower(root.get("address")), "%" + address.toLowerCase() + "%");
    }

    /**
     * Filters users by their alerting flag.
     *
     * @param alerting the alerting value, or {@code null} to skip
     * @return the specification, or {@code null} if alerting is null
     */
    public static Specification<User> alertingEquals(Boolean alerting) {
        return (root, query, cb) -> alerting == null
                ? null
                : cb.equal(root.get("alerting"), alerting);
    }

    /**
     * Filters users whose energy alerting threshold is greater than or equal to the given value.
     *
     * @param threshold the minimum threshold, or {@code null} to skip
     * @return the specification, or {@code null} if the threshold is null
     */
    public static Specification<User> thresholdGreaterThanOrEqual(Double threshold) {
        return (root, query, cb) -> threshold == null
                ? null
                : cb.greaterThanOrEqualTo(root.get("energyAlertingThreshold"), threshold);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}