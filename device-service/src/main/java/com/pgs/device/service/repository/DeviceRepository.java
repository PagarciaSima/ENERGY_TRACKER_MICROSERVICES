package com.pgs.device.service.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.pgs.device.service.entity.Device;

/**
 * Spring Data repository for {@link Device} entities.
 * <p>
 * Besides the standard CRUD operations it exposes finders by owner user id, both
 * paginated and unpaginated.
 */
@Repository
public interface DeviceRepository
        extends JpaRepository<Device, Long>, JpaSpecificationExecutor<Device> {

    /**
     * Retrieves every device owned by the given user.
     *
     * @param userId the owner user id
     * @return the list of devices owned by the user
     */
    List<Device> findAllByUserId(Long userId);

    /**
     * Retrieves a page of devices owned by the given user.
     *
     * @param userId   the owner user id
     * @param pageable the pagination information
     * @return a page of devices owned by the user
     */
    Page<Device> findAllByUserId(Long userId, Pageable pageable);
}