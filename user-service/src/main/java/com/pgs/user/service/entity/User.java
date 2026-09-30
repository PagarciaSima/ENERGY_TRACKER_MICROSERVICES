package com.pgs.user.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name must not exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String name;

    /** Last name of the user. */
    @NotBlank(message = "surname is required")
    @Size(max = 100, message = "surname must not exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String surname;

    /** Email address of the user. */
    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    @Size(max = 150, message = "email must not exceed 150 characters")
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /** Physical address of the user. */
    @Size(max = 255, message = "address must not exceed 255 characters")
    @Column(length = 255)
    private String address;

    /** Indicates whether alerting is enabled for the user. */
    @Column(nullable = false)
    private boolean alerting;

    /** Threshold value that triggers an energy alert. */
    @DecimalMin(value = "0.0", inclusive = true, message = "energyAlertingThreshold must be greater than or equal to 0")
    @Column(nullable = false)
    private double energyAlertingThreshold;

}