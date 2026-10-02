package com.pgs.device.service.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.pgs.device.service.entity.Device;

@Repository
public interface DeviceRepository
        extends JpaRepository<Device, Long>, JpaSpecificationExecutor<Device> {

    List<Device> findAllByUserId(Long userId);

    Page<Device> findAllByUserId(Long userId, Pageable pageable);
}