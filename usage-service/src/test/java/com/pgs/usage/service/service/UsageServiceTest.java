package com.pgs.usage.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.pgs.kafka.event.AlertingEvent;
import com.pgs.kafka.event.EnergyUsageEvent;
import com.pgs.usage.service.client.DeviceClient;
import com.pgs.usage.service.client.UserClient;
import com.pgs.usage.service.dto.DeviceDto;
import com.pgs.usage.service.dto.UsageDto;
import com.pgs.usage.service.dto.UserDto;

/**
 * Unit tests for {@link UsageService}.
 * <p>
 * All collaborators (InfluxDB, the Device/User clients and Kafka) are mocked, so
 * the suite runs without any external infrastructure. The tests cover the
 * aggregation query, the scheduled threshold evaluation, the handling of users
 * that no longer exist (404) and the per-user alert cooldown, which is verified
 * with a controllable {@link MutableClock}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UsageService")
class UsageServiceTest {

    private static final String BUCKET = "usage-bucket";
    private static final String ORG = "home-energy-tracker";
    private static final long COOLDOWN_MINUTES = 60L;
    private static final Instant NOW = Instant.parse("2026-10-29T15:00:00Z");

    @Mock
    private InfluxDBClient influxDBClient;

    @Mock
    private QueryApi queryApi;

    @Mock
    private DeviceClient deviceClient;

    @Mock
    private UserClient userClient;

    @Mock
    private KafkaTemplate<String, AlertingEvent> kafkaTemplate;

    private MutableClock clock;
    private UsageService usageService;

    /**
     * Builds the service under test with mocked collaborators and a controllable
     * clock.
     */
    @BeforeEach
    void setUp() {
        clock = new MutableClock(NOW);
        usageService = new UsageService(influxDBClient, deviceClient, userClient, kafkaTemplate, clock);
        ReflectionTestUtils.setField(usageService, "influxBucket", BUCKET);
        ReflectionTestUtils.setField(usageService, "influxOrg", ORG);
        ReflectionTestUtils.setField(usageService, "alertCooldownMinutes", COOLDOWN_MINUTES);
    }

    /**
     * Builds a device owned by the given user.
     *
     * @param id     the device id
     * @param userId the owner user id
     * @return the device DTO
     */
    private static DeviceDto device(Long id, Long userId) {
        return DeviceDto.builder().id(id).name("Device " + id).type("LIGHT").userId(userId).build();
    }

    /**
     * Builds a Flux table holding a single aggregated record.
     *
     * @param deviceId the value of the {@code deviceId} tag
     * @param value    the aggregated consumption
     * @return the Flux table
     */
    private static FluxTable fluxTable(String deviceId, double value) {
        final FluxRecord record = new FluxRecord(0);
        record.getValues().put("deviceId", deviceId);
        record.getValues().put("_value", value);
        final FluxTable table = new FluxTable();
        table.getRecords().add(record);
        return table;
    }

    /**
     * Builds an alerting-enabled user with the given threshold.
     *
     * @param id        the user id
     * @param threshold the energy alerting threshold
     * @return the user DTO
     */
    private static UserDto alertingUser(Long id, double threshold) {
        return UserDto.builder()
                .id(id)
                .name("Name")
                .surname("Surname")
                .email("user" + id + "@example.com")
                .alerting(true)
                .energyAlertingThreshold(threshold)
                .build();
    }

    /**
     * Builds the 404 exception raised by {@code RestClient} when the User Service
     * reports a deleted user.
     *
     * @return the not-found exception
     */
    private static HttpClientErrorException notFound() {
        return HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, null, null);
    }

    /**
     * Builds a 500 exception, simulating a failing User Service.
     *
     * @return the server error exception
     */
    private static HttpServerErrorException serverError() {
        return HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                HttpHeaders.EMPTY, null, null);
    }

    @Nested
    @DisplayName("getXDaysUsageForUser")
    class GetXDaysUsageForUser {

        @Test
        @DisplayName("returns every device with its aggregated consumption")
        void returnsEveryDeviceWithItsAggregatedConsumption() {
            when(deviceClient.getAllDevicesForUser(1L)).thenReturn(List.of(device(1L, 1L), device(2L, 1L)));
            when(influxDBClient.getQueryApi()).thenReturn(queryApi);
            when(queryApi.query(anyString(), eq(ORG)))
                    .thenReturn(List.of(fluxTable("1", 2.5), fluxTable("2", 1.5)));

            UsageDto usage = usageService.getXDaysUsageForUser(1L, 3);

            assertThat(usage.userId()).isEqualTo(1L);
            assertThat(usage.devices()).hasSize(2);
            assertThat(usage.devices()).extracting(DeviceDto::energyConsumed).containsExactly(2.5, 1.5);
        }

        @Test
        @DisplayName("returns an empty device list when the user has no devices")
        void returnsEmptyDeviceListWhenUserHasNoDevices() {
            when(deviceClient.getAllDevicesForUser(1L)).thenReturn(List.of());

            UsageDto usage = usageService.getXDaysUsageForUser(1L, 3);

            assertThat(usage.userId()).isEqualTo(1L);
            assertThat(usage.devices()).isEmpty();
            verify(influxDBClient, never()).getQueryApi();
        }

        @Test
        @DisplayName("defaults the consumption to zero when InfluxDB has no data")
        void defaultsConsumptionToZeroWhenInfluxHasNoData() {
            when(deviceClient.getAllDevicesForUser(1L)).thenReturn(List.of(device(1L, 1L)));
            when(influxDBClient.getQueryApi()).thenReturn(queryApi);
            when(queryApi.query(anyString(), eq(ORG))).thenReturn(List.of());

            UsageDto usage = usageService.getXDaysUsageForUser(1L, 3);

            assertThat(usage.devices()).singleElement()
                    .extracting(DeviceDto::energyConsumed).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("aggregateDeviceEnergyUsage")
    class AggregateDeviceEnergyUsage {

        /**
         * Stubs InfluxDB so that one device reports the given aggregated
         * consumption, and resolves that device to the given user.
         *
         * @param deviceId    the device reporting the consumption
         * @param userId      the owner of the device
         * @param consumption the aggregated consumption
         */
        private void givenAggregatedConsumptionFor(Long deviceId, Long userId, double consumption) {
            when(influxDBClient.getQueryApi()).thenReturn(queryApi);
            when(queryApi.query(anyString(), eq(ORG)))
                    .thenReturn(List.of(fluxTable(String.valueOf(deviceId), consumption)));
            when(deviceClient.getDeviceById(deviceId)).thenReturn(device(deviceId, userId));
        }

        @Test
        @DisplayName("publishes an alert event when the threshold is exceeded")
        void publishesAlertWhenThresholdIsExceeded() {
            givenAggregatedConsumptionFor(10L, 1L, 50.0);
            when(userClient.getUserById(1L)).thenReturn(alertingUser(1L, 10.0));

            usageService.aggregateDeviceEnergyUsage();

            ArgumentCaptor<AlertingEvent> captor = ArgumentCaptor.forClass(AlertingEvent.class);
            verify(kafkaTemplate).send(eq("energy-alerts"), captor.capture());
            assertThat(captor.getValue().userId()).isEqualTo(1L);
            assertThat(captor.getValue().threshold()).isEqualTo(10.0);
            assertThat(captor.getValue().energyConsumed()).isEqualTo(50.0);
            assertThat(captor.getValue().email()).isEqualTo("user1@example.com");
            assertThat(captor.getValue().message()).isEqualTo("Energy consumption threshold exceeded");
        }

        @Test
        @DisplayName("does not publish anything when the consumption is within the threshold")
        void doesNotPublishWhenWithinThreshold() {
            givenAggregatedConsumptionFor(10L, 1L, 5.0);
            when(userClient.getUserById(1L)).thenReturn(alertingUser(1L, 10.0));

            usageService.aggregateDeviceEnergyUsage();

            verify(kafkaTemplate, never()).send(anyString(), any());
        }

        @Test
        @DisplayName("skips deleted users (404) without failing")
        void skipsDeletedUsersWithoutFailing() {
            givenAggregatedConsumptionFor(10L, 1L, 50.0);
            when(userClient.getUserById(1L)).thenThrow(notFound());

            assertThatCode(() -> usageService.aggregateDeviceEnergyUsage()).doesNotThrowAnyException();

            verify(kafkaTemplate, never()).send(anyString(), any());
        }

        @Test
        @DisplayName("keeps running when the User Service fails")
        void keepsRunningWhenUserServiceFails() {
            givenAggregatedConsumptionFor(10L, 1L, 50.0);
            when(userClient.getUserById(1L)).thenThrow(serverError());

            assertThatCode(() -> usageService.aggregateDeviceEnergyUsage()).doesNotThrowAnyException();

            verify(kafkaTemplate, never()).send(anyString(), any());
        }

        @Test
        @DisplayName("suppresses repeated alerts while the cooldown is active")
        void suppressesRepeatedAlertsWhileCooldownIsActive() {
            givenAggregatedConsumptionFor(10L, 1L, 50.0);
            when(userClient.getUserById(1L)).thenReturn(alertingUser(1L, 10.0));

            usageService.aggregateDeviceEnergyUsage();
            clock.advance(Duration.ofMinutes(COOLDOWN_MINUTES - 1));
            usageService.aggregateDeviceEnergyUsage();

            verify(kafkaTemplate, times(1)).send(anyString(), any());
        }

        @Test
        @DisplayName("publishes again once the cooldown has expired")
        void publishesAgainOnceCooldownExpires() {
            givenAggregatedConsumptionFor(10L, 1L, 50.0);
            when(userClient.getUserById(1L)).thenReturn(alertingUser(1L, 10.0));

            usageService.aggregateDeviceEnergyUsage();
            clock.advance(Duration.ofMinutes(COOLDOWN_MINUTES + 1));
            usageService.aggregateDeviceEnergyUsage();

            verify(kafkaTemplate, times(2)).send(anyString(), any());
        }

        @Test
        @DisplayName("applies the cooldown per user")
        void appliesCooldownPerUser() {
            when(influxDBClient.getQueryApi()).thenReturn(queryApi);
            when(queryApi.query(anyString(), eq(ORG))).thenReturn(List.of(
                    fluxTable("10", 50.0),
                    fluxTable("20", 60.0)));
            when(deviceClient.getDeviceById(10L)).thenReturn(device(10L, 1L));
            when(deviceClient.getDeviceById(20L)).thenReturn(device(20L, 2L));
            when(userClient.getUserById(1L)).thenReturn(alertingUser(1L, 10.0));
            when(userClient.getUserById(2L)).thenReturn(alertingUser(2L, 10.0));

            usageService.aggregateDeviceEnergyUsage();
            usageService.aggregateDeviceEnergyUsage();

            verify(kafkaTemplate, times(2)).send(anyString(), any());
        }
    }

    @Nested
    @DisplayName("energyUsageEvent")
    class EnergyUsageEventListener {

        @Test
        @DisplayName("writes the reading to InfluxDB")
        void writesReadingToInfluxDb() {
            final WriteApiBlocking writeApi = mock(WriteApiBlocking.class);
            when(influxDBClient.getWriteApiBlocking()).thenReturn(writeApi);

            usageService.energyUsageEvent(new EnergyUsageEvent(10L, 2.5, NOW));

            verify(writeApi).writePoint(eq(BUCKET), eq(ORG), any(Point.class));
        }

        @Test
        @DisplayName("propagates write failures so Kafka can retry")
        void propagatesWriteFailures() {
            final WriteApiBlocking writeApi = mock(WriteApiBlocking.class);
            when(influxDBClient.getWriteApiBlocking()).thenReturn(writeApi);
            doThrow(new IllegalStateException("InfluxDB down")).when(writeApi)
                    .writePoint(anyString(), anyString(), any(Point.class));

            assertThatThrownBy(() -> usageService.energyUsageEvent(new EnergyUsageEvent(10L, 2.5, NOW)))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    /**
     * Mutable {@link Clock} that lets the tests control the passage of time.
     */
    private static final class MutableClock extends Clock {

        private final ZoneId zone = ZoneOffset.UTC;
        private Instant instant;

        /**
         * Creates a clock frozen at the given instant.
         *
         * @param instant the initial instant
         */
        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        /**
         * Moves the clock forward by the given duration.
         *
         * @param duration the amount of time to advance
         */
        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
