package com.pgs.device.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.pgs.device.service.dto.DeviceDto;
import com.pgs.device.service.entity.Device;
import com.pgs.device.service.model.DeviceType;
import com.pgs.device.service.repository.DeviceRepository;

/**
 * Verifies that the {@code @Cacheable} / {@code @CacheEvict} annotations on
 * {@link DeviceService} behave as documented, without requiring Redis.
 * <p>
 * A minimal Spring context is loaded with {@link EnableCaching} and an
 * in-memory {@link ConcurrentMapCacheManager}. The repository is mocked.
 * The service is a real Spring bean, so the cache proxy is created the same
 * way it would be in production.
 * <p>
 * The in-memory cache is cleared before each test so that tests are
 * independent and deterministic, regardless of execution order.
 */
@SpringJUnitConfig(DeviceServiceCacheTest.TestConfig.class)
class DeviceServiceCacheTest {

    private static final String CACHE_NAME = "devices";
    private static final Long DEVICE_ID = 1L;

    @TestConfiguration
    @EnableCaching
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(CACHE_NAME);
        }

        @Bean
        DeviceService deviceService(DeviceRepository deviceRepository) {
            return new DeviceService(deviceRepository);
        }
    }

    @MockitoBean
    private DeviceRepository deviceRepository;

    @Autowired
    private DeviceService service;

    @Autowired
    private CacheManager cacheManager;

    /**
     * Clears the shared in-memory cache before each test so that state from
     * previous tests does not leak into the current one.
     */
    @BeforeEach
    void clearCache() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.clear();
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Test data                                                          */
    /* ------------------------------------------------------------------ */

    private static Device sampleDevice(Long id) {
        return Device.builder()
                .id(id)
                .name("Smart Bulb Salón")
                .type(DeviceType.LIGHT)
                .location("Salón principal")
                .userId(1L)
                .build();
    }

    private static DeviceDto sampleUpdate() {
        return DeviceDto.builder()
                .name("Smart Bulb Cocina")
                .type(DeviceType.LIGHT)
                .location("Cocina")
                .userId(2L)
                .build();
    }
    /* ------------------------------------------------------------------ */
    /*  @Cacheable on getDeviceById                                        */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("@Cacheable getDeviceById")
    class GetDeviceByIdCacheable {

        @Test
        void cachesFirstResult_soSecondReadSkipsRepository() {
            when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(sampleDevice(DEVICE_ID)));

            DeviceDto first = service.getDeviceById(DEVICE_ID);
            DeviceDto second = service.getDeviceById(DEVICE_ID);

            assertThat(first.getId()).isEqualTo(DEVICE_ID);
            assertThat(second.getId()).isEqualTo(DEVICE_ID);
            verify(deviceRepository, times(1)).findById(DEVICE_ID);
        }

        @Test
        void populatesCacheUnderIdKey() {
            when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(sampleDevice(DEVICE_ID)));

            service.getDeviceById(DEVICE_ID);

            Cache cache = cacheManager.getCache(CACHE_NAME);
            assertThat(cache).isNotNull();
            assertThat(cache.get(DEVICE_ID)).isNotNull();
            assertThat(cache.get(DEVICE_ID).get()).isInstanceOf(DeviceDto.class);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  @CacheEvict on updateDevice                                        */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("@CacheEvict updateDevice")
    class UpdateDeviceCacheEvict {

        @Test
        void evictsCachedEntry_soNextReadHitsRepositoryAgain() {
            when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(sampleDevice(DEVICE_ID)));

            service.getDeviceById(DEVICE_ID);                 // cache miss → findById (hit 1)
            service.updateDevice(DEVICE_ID, sampleUpdate());  // findById (hit 2) + evict
            service.getDeviceById(DEVICE_ID);                 // cache miss → findById (hit 3)

            verify(deviceRepository, times(3)).findById(DEVICE_ID);
        }

        @Test
        void leavesCacheEmptyAfterEviction() {
            when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(sampleDevice(DEVICE_ID)));

            service.getDeviceById(DEVICE_ID);
            service.updateDevice(DEVICE_ID, sampleUpdate());

            Cache cache = cacheManager.getCache(CACHE_NAME);
            assertThat(cache).isNotNull();
            assertThat(cache.get(DEVICE_ID)).isNull();
        }
    }

    /* ------------------------------------------------------------------ */
    /*  @CacheEvict on deleteDevice                                        */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("@CacheEvict deleteDevice")
    class DeleteDeviceCacheEvict {

        @Test
        void evictsCachedEntry_andRepopulatesOnNextRead() {
            when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(sampleDevice(DEVICE_ID)));

            service.getDeviceById(DEVICE_ID);                 // cache miss → findById (hit 1)
            when(deviceRepository.existsById(DEVICE_ID)).thenReturn(true);
            service.deleteDevice(DEVICE_ID);                  // evict
            verify(deviceRepository).deleteById(DEVICE_ID);

            service.getDeviceById(DEVICE_ID);                 // cache miss → findById (hit 2)
            service.getDeviceById(DEVICE_ID);                 // cache hit → no findById

            verify(deviceRepository, times(2)).findById(DEVICE_ID);
        }

        @Test
        void leavesCacheEmptyAfterEviction() {
            when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(sampleDevice(DEVICE_ID)));

            service.getDeviceById(DEVICE_ID);
            when(deviceRepository.existsById(DEVICE_ID)).thenReturn(true);
            service.deleteDevice(DEVICE_ID);

            Cache cache = cacheManager.getCache(CACHE_NAME);
            assertThat(cache).isNotNull();
            assertThat(cache.get(DEVICE_ID)).isNull();
        }
    }
}