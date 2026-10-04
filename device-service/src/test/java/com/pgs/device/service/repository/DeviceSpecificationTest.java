package com.pgs.device.service.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.TestPropertySource;

import com.pgs.device.service.entity.Device;
import com.pgs.device.service.model.DeviceType;

/**
 * Slice tests for {@link DeviceSpecification} against an embedded H2 database.
 * <p>
 * The production datasource points to MySQL (see {@code application.properties}),
 * so the embedded replacement is disabled and H2 is configured explicitly in
 * MySQL compatibility mode. This lets the Flyway migration
 * {@code V1__device_table.sql} (MySQL-specific DDL) run unchanged. Each test is
 * transactional and rolled back by {@link DataJpaTest}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:device_spec_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;NON_KEYWORDS=DEVICE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration"
})
class DeviceSpecificationTest {

    private static final String BOMBILLA = "Bombilla Salón";
    private static final String TERMOSTATO = "Termostato";
    private static final String CAMARA = "Cámara Cocina";

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private TestEntityManager entityManager;

    /**
     * Persists the three fixture devices used by most tests:
     * <ul>
     *     <li>Bombilla Salón (LIGHT, Salón principal, user 1)</li>
     *     <li>Termostato (THERMOSTAT, Salón principal, user 1)</li>
     *     <li>Cámara Cocina (CAMERA, Cocina, user 2)</li>
     * </ul>
     */
    private void seedDevices() {
        persistDevice(BOMBILLA, DeviceType.LIGHT, "Salón principal", 1L);
        persistDevice(TERMOSTATO, DeviceType.THERMOSTAT, "Salón principal", 1L);
        persistDevice(CAMARA, DeviceType.CAMERA, "Cocina", 2L);
    }

    private void persistDevice(String name, DeviceType type, String location, Long userId) {
        entityManager.persistAndFlush(Device.builder()
                .name(name)
                .type(type)
                .location(location)
                .userId(userId)
                .build());
    }

    private List<String> namesOf(Specification<Device> spec) {
        return deviceRepository.findAll(spec).stream()
                .map(Device::getName)
                .toList();
    }
    /* ------------------------------------------------------------------ */
    /*  nameContains                                                     */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("nameContains")
    class NameContains {

        @Test
        void matchesNameContainingCaseInsensitive() {
            seedDevices();

            assertThat(namesOf(DeviceSpecification.nameContains("bombilla")))
                    .containsExactly(BOMBILLA);
        }

        @Test
        void blankValueReturnsAllDevices() {
            seedDevices();

            assertThat(namesOf(DeviceSpecification.nameContains("   ")))
                    .containsExactlyInAnyOrder(BOMBILLA, TERMOSTATO, CAMARA);
        }

        @Test
        void nullValueReturnsAllDevices() {
            seedDevices();

            assertThat(namesOf(DeviceSpecification.nameContains(null)))
                    .containsExactlyInAnyOrder(BOMBILLA, TERMOSTATO, CAMARA);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  locationContains                                                  */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("locationContains")
    class LocationContains {

        @Test
        void matchesLocationContainingCaseInsensitive() {
            seedDevices();

            assertThat(namesOf(DeviceSpecification.locationContains("COCINA")))
                    .containsExactly(CAMARA);
        }

        @Test
        void nullValueReturnsAllDevices() {
            seedDevices();

            assertThat(namesOf(DeviceSpecification.locationContains(null)))
                    .containsExactlyInAnyOrder(BOMBILLA, TERMOSTATO, CAMARA);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  typeEquals                                                        */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("typeEquals")
    class TypeEquals {

        @Test
        void matchesExactType() {
            seedDevices();

            assertThat(namesOf(DeviceSpecification.typeEquals(DeviceType.LIGHT)))
                    .containsExactly(BOMBILLA);
        }

        @Test
        void nullValueReturnsAllDevices() {
            seedDevices();

            assertThat(namesOf(DeviceSpecification.typeEquals(null)))
                    .containsExactlyInAnyOrder(BOMBILLA, TERMOSTATO, CAMARA);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  userIdEquals                                                      */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("userIdEquals")
    class UserIdEquals {

        @Test
        void matchesDevicesOfThatUser() {
            seedDevices();

            assertThat(namesOf(DeviceSpecification.userIdEquals(1L)))
                    .containsExactlyInAnyOrder(BOMBILLA, TERMOSTATO);
        }

        @Test
        void nullValueReturnsAllDevices() {
            seedDevices();

            assertThat(namesOf(DeviceSpecification.userIdEquals(null)))
                    .containsExactlyInAnyOrder(BOMBILLA, TERMOSTATO, CAMARA);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Combined specifications                                            */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("combined specifications")
    class CombinedSpecifications {

        @Test
        void andCombinesPredicates() {
            seedDevices();

            Specification<Device> spec = DeviceSpecification.locationContains("salón")
                    .and(DeviceSpecification.typeEquals(DeviceType.THERMOSTAT));

            assertThat(namesOf(spec)).containsExactly(TERMOSTATO);
        }

        @Test
        void andWithUserIdNarrowsBothMatches() {
            seedDevices();

            Specification<Device> spec = DeviceSpecification.nameContains("a")
                    .and(DeviceSpecification.userIdEquals(1L));

            assertThat(namesOf(spec))
                    .containsExactlyInAnyOrder(BOMBILLA, TERMOSTATO);
        }
    }
}