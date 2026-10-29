package com.pgs.alert.service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pgs.alert.service.entity.Alert;

/**
 * Spring Data repository for {@link Alert} entities.
 * <p>
 * Provides the standard CRUD operations used to persist the notification audit
 * trail.
 */
public interface AlertRepository  extends JpaRepository<Alert, Long> {
}
