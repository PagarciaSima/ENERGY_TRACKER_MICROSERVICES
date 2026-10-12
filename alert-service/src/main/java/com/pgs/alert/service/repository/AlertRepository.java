package com.pgs.alert.service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pgs.alert.service.entity.Alert;

public interface AlertRepository  extends JpaRepository<Alert, Long> {
}
