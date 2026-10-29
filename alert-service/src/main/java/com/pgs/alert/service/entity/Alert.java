package com.pgs.alert.service.entity;

import java.time.Instant;

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
 * JPA entity that stores the audit trail of the alert notifications sent by the
 * service, including whether the email was delivered successfully.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "alert")
public class Alert {

    /** The alert record id (auto-generated). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The id of the user the alert belongs to. */
    private Long userId;

    /** A human-readable description of the alert. */
    private String message;

    /** The consumption threshold that was exceeded. */
    private Double threshold;

    /** The aggregated consumption that triggered the alert. */
    private Double energyConsumed;

    /** The email address the notification was sent to. */
    private String email;

    /** The instant at which the alert record was created. */
    private Instant createdAt;

    /** Whether the notification email was delivered successfully. */
    private boolean sent;
}