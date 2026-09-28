package com.pgs.user.service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA entity representing a user.
 */
@Entity
@Table(name = "user")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class User {

    /** Unique identifier of the user. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** First name of the user. */
    private String name;

    /** Last name of the user. */
    private String surname;

    /** Email address of the user. */
    private String email;

    /** Physical address of the user. */
    private String address;

    /** Indicates whether alerting is enabled for the user. */
    private boolean alerting;

    /** Threshold value that triggers an energy alert. */
    private double energyAlertingThreshold;

}