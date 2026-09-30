package com.pgs.user.service.service;

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

import com.pgs.user.service.dto.UserDto;
import com.pgs.user.service.entity.User;
import com.pgs.user.service.repository.UserRepository;

/**
 * Verifies that the {@code @Cacheable} / {@code @CacheEvict} annotations on
 * {@link UserService} behave as documented, without requiring Redis.
 * <p>
 * A minimal Spring context is loaded with {@link EnableCaching} and an
 * in-memory {@link ConcurrentMapCacheManager}. The repository is mocked.
 * The service is a real Spring bean, so the cache proxy is created the same
 * way it would be in production.
 * <p>
 * The in-memory cache is cleared before each test so that tests are
 * independent and deterministic, regardless of execution order.
 */
@SpringJUnitConfig(UserServiceCacheTest.TestConfig.class)
class UserServiceCacheTest {

    private static final String CACHE_NAME = "users";
    private static final Long USER_ID = 1L;

    @TestConfiguration
    @EnableCaching
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(CACHE_NAME);
        }

        @Bean
        UserService userService(UserRepository userRepository) {
            return new UserService(userRepository);
        }
    }

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private UserService service;

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

    private static User sampleUser(Long id) {
        return User.builder()
                .id(id)
                .name("Ana")
                .surname("García")
                .email("ana.garcia@example.com")
                .address("Calle Mayor 5, Madrid")
                .alerting(true)
                .energyAlertingThreshold(3200.5)
                .build();
    }

    private static UserDto sampleUpdate() {
        return UserDto.builder()
                .name("Ana")
                .surname("García")
                .email("ana.garcia@example.com")
                .address("Calle Mayor 5, Madrid")
                .alerting(false)
                .energyAlertingThreshold(3000.0)
                .build();
    }

    /* ------------------------------------------------------------------ */
    /*  @Cacheable on getUserById                                          */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("@Cacheable getUserById")
    class GetUserByIdCacheable {

        @Test
        void secondCall_isServedFromCache_andRepositoryIsHitOnlyOnce() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(sampleUser(USER_ID)));

            UserDto first = service.getUserById(USER_ID);
            UserDto second = service.getUserById(USER_ID);

            assertThat(first.getId()).isEqualTo(USER_ID);
            assertThat(second.getId()).isEqualTo(USER_ID);
            verify(userRepository, times(1)).findById(USER_ID);
        }

        @Test
        void populatesCacheUnderIdKey() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(sampleUser(USER_ID)));

            service.getUserById(USER_ID);

            Cache cache = cacheManager.getCache(CACHE_NAME);
            assertThat(cache).isNotNull();
            assertThat(cache.get(USER_ID)).isNotNull();
            assertThat(cache.get(USER_ID).get()).isInstanceOf(UserDto.class);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  @CacheEvict on updateUser                                          */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("@CacheEvict updateUser")
    class UpdateUserCacheEvict {

        @Test
        void evictsCachedEntry_soNextReadHitsRepositoryAgain() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(sampleUser(USER_ID)));

            service.getUserById(USER_ID);                 // cache miss → findById (hit 1)
            service.updateUser(USER_ID, sampleUpdate());  // findById (hit 2) + evict
            service.getUserById(USER_ID);                 // cache miss → findById (hit 3)

            verify(userRepository, times(3)).findById(USER_ID);
        }

        @Test
        void leavesCacheEmptyAfterEviction() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(sampleUser(USER_ID)));

            service.getUserById(USER_ID);
            service.updateUser(USER_ID, sampleUpdate());

            Cache cache = cacheManager.getCache(CACHE_NAME);
            assertThat(cache).isNotNull();
            assertThat(cache.get(USER_ID)).isNull();
        }
    }

    /* ------------------------------------------------------------------ */
    /*  @CacheEvict on deleteUser                                          */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("@CacheEvict deleteUser")
    class DeleteUserCacheEvict {

        @Test
        void evictsCachedEntry_andRepopulatesOnNextRead() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(sampleUser(USER_ID)));

            service.getUserById(USER_ID);                 // cache miss → findById (hit 1)
            when(userRepository.existsById(USER_ID)).thenReturn(true);
            service.deleteUser(USER_ID);                  // evict
            verify(userRepository).deleteById(USER_ID);

            service.getUserById(USER_ID);                 // cache miss → findById (hit 2)
            service.getUserById(USER_ID);                 // cache hit → no findById

            verify(userRepository, times(2)).findById(USER_ID);
        }

        @Test
        void leavesCacheEmptyAfterEviction() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(sampleUser(USER_ID)));

            service.getUserById(USER_ID);
            when(userRepository.existsById(USER_ID)).thenReturn(true);
            service.deleteUser(USER_ID);

            Cache cache = cacheManager.getCache(CACHE_NAME);
            assertThat(cache).isNotNull();
            assertThat(cache.get(USER_ID)).isNull();
        }
    }
}