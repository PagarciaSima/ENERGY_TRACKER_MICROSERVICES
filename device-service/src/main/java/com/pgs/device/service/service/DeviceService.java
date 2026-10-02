package com.pgs.device.service.service;

import java.util.Objects;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pgs.device.service.dto.DeviceDto;
import com.pgs.device.service.dto.DeviceFilterDto;
import com.pgs.device.service.dto.PageResponse;
import com.pgs.device.service.entity.Device;
import com.pgs.device.service.exception.DeviceNotFoundException;
import com.pgs.device.service.repository.DeviceRepository;
import com.pgs.device.service.repository.DeviceSpecification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service layer for managing devices.
 * <p>
 * Reads are cached in the {@code devices} cache (Redis) and invalidated on
 * writes, following the same pattern as {@code UserService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService {

    private static final String CACHE_DEVICES = "devices";

    private final DeviceRepository deviceRepository;

    /* ------------------------------------------------------------------ */
    /*  CREATE                                                             */
    /* ------------------------------------------------------------------ */

    /**
     * Creates a new device from the provided data.
     *
     * @param input the device data to create
     * @return the created device as a DTO
     */
    @Transactional
    public DeviceDto createDevice(DeviceDto input) {
        Device device = Device.builder()
                .name(input.getName())
                .type(input.getType())
                .location(input.getLocation())
                .userId(input.getUserId())
                .build();

        Device saved = deviceRepository.save(device);
        log.debug("Created device with id={}", saved.getId());
        return toDto(saved);
    }

    /* ------------------------------------------------------------------ */
    /*  READ                                                               */
    /* ------------------------------------------------------------------ */

    /**
     * Retrieves a device by ID, using cache when available.
     *
     * @param id the device ID
     * @return the device as a DTO
     * @throws DeviceNotFoundException if the device does not exist
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_DEVICES, key = "#id")
    public DeviceDto getDeviceById(Long id) {
        return deviceRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new DeviceNotFoundException(id));
    }

    /**
     * Retrieves all devices, paginated.
     *
     * @param page the page index (zero-based)
     * @param size the page size
     * @return a page of devices as DTOs
     */
    @Transactional(readOnly = true)
    public PageResponse<DeviceDto> getAllDevices(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return PageResponse.of(deviceRepository.findAll(pageable).map(this::toDto));
    }

    /**
     * Retrieves all devices belonging to a given user, paginated.
     *
     * @param userId the user ID
     * @param page   the page index (zero-based)
     * @param size   the page size
     * @return a page of devices for the user
     */
    @Transactional(readOnly = true)
    public PageResponse<DeviceDto> getDevicesByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return PageResponse.of(
                deviceRepository.findAllByUserId(userId, pageable).map(this::toDto));
    }

    /**
     * Searches devices by optional filters, paginated.
     *
     * @param filter the filter criteria
     * @param page   the page index (zero-based)
     * @param size   the page size
     * @return a page of matching devices as DTOs
     */
    @Transactional(readOnly = true)
    public PageResponse<DeviceDto> searchDevices(DeviceFilterDto filter, int page, int size) {
        Specification<Device> spec = buildSpecification(filter);
        Pageable pageable = PageRequest.of(page, size);
        return PageResponse.of(deviceRepository.findAll(spec, pageable).map(this::toDto));
    }

    /* ------------------------------------------------------------------ */
    /*  UPDATE                                                             */
    /* ------------------------------------------------------------------ */

    /**
     * Updates an existing device with the provided data and evicts the cache entry.
     *
     * @param id    the device ID
     * @param input the updated device data
     * @return the updated device as a DTO
     * @throws DeviceNotFoundException if the device does not exist
     */
    @Transactional
    @CacheEvict(value = CACHE_DEVICES, key = "#id")
    public DeviceDto updateDevice(Long id, DeviceDto input) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new DeviceNotFoundException(id));

        device.setName(input.getName());
        device.setType(input.getType());
        device.setLocation(input.getLocation());
        device.setUserId(input.getUserId());

        // Entity is managed by the EntityManager; no explicit save() needed.
        log.debug("Updated device with id={}", id);
        return toDto(device);
    }

    /* ------------------------------------------------------------------ */
    /*  DELETE                                                             */
    /* ------------------------------------------------------------------ */

    /**
     * Deletes a device by ID and evicts the cache entry.
     *
     * @param id the device ID
     * @throws DeviceNotFoundException if the device does not exist
     */
    @Transactional
    @CacheEvict(value = CACHE_DEVICES, key = "#id")
    public void deleteDevice(Long id) {
        if (!deviceRepository.existsById(id)) {
            throw new DeviceNotFoundException(id);
        }
        deviceRepository.deleteById(id);
        log.debug("Deleted device with id={}", id);
    }

    /* ------------------------------------------------------------------ */
    /*  HELPERS                                                            */
    /* ------------------------------------------------------------------ */

    /**
     * Builds the JPA specification from the filter, skipping null/blank fields.
     *
     * @param filter the filter criteria
     * @return the combined specification (never {@code null})
     */
    private Specification<Device> buildSpecification(DeviceFilterDto filter) {
        Specification<Device> spec = (root, query, cb) -> cb.conjunction();
        if (filter == null) {
            return spec;
        }

        spec = spec.and(DeviceSpecification.nameContains(filter.getName()))
                   .and(DeviceSpecification.typeEquals(filter.getType()))
                   .and(DeviceSpecification.locationContains(filter.getLocation()))
                   .and(DeviceSpecification.userIdEquals(filter.getUserId()));

        return spec;
    }

    /**
     * Maps a {@link Device} entity to a {@link DeviceDto}.
     *
     * @param device the device entity
     * @return the corresponding device DTO
     */
    private DeviceDto toDto(Device device) {
        Objects.requireNonNull(device, "device must not be null");
        return DeviceDto.builder()
                .id(device.getId())
                .name(device.getName())
                .type(device.getType())
                .location(device.getLocation())
                .userId(device.getUserId())
                .build();
    }
}