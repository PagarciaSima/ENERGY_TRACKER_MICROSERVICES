package com.pgs.usage.service.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
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
import com.pgs.usage.service.model.DeviceEnergy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for handling energy usage data.
 * <p>
 * It consumes energy usage events from Kafka, stores them in InfluxDB,
 * periodically aggregates device energy consumption, checks thresholds for
 * users, and sends alerts when exceeded. It also provides a method to retrieve
 * aggregated usage for a user over a specified number of days.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UsageService {

	private static final String TOPIC_ENERGY_USAGE = "energy-usage";
	private static final String TOPIC_ENERGY_ALERTS = "energy-alerts";
	private static final String MEASUREMENT_ENERGY_USAGE = "energy_usage";
	private static final String FIELD_ENERGY_CONSUMED = "energyConsumed";
	private static final String TAG_DEVICE_ID = "deviceId";
	private static final String FLUX_VALUE_COLUMN = "_value";
	private static final long ONE_HOUR_IN_SECONDS = 3600L;
	private static final long SECONDS_PER_DAY = 24L * 3600L;
	private static final String ALERT_MESSAGE_THRESHOLD_EXCEEDED = "Energy consumption threshold exceeded";

	private final InfluxDBClient influxDBClient;
	private final DeviceClient deviceClient;
	private final UserClient userClient;
	private final KafkaTemplate<String, AlertingEvent> kafkaTemplate;

	@Value("${influx.bucket}")
	private String influxBucket;

	@Value("${influx.org}")
	private String influxOrg;

	// ------------------------------------------------------------------
	// Kafka listener: persist incoming readings
	// ------------------------------------------------------------------

	/**
	 * Kafka listener that consumes {@link EnergyUsageEvent} messages from the
	 * "energy-usage" topic and writes them to InfluxDB.
	 * <p>
	 * If the write fails, the error is logged and re-thrown so Kafka can apply
	 * its retry / DLQ policy.
	 *
	 * @param event the energy usage event containing device ID, energy consumed,
	 *              and timestamp
	 */
	@KafkaListener(topics = TOPIC_ENERGY_USAGE, groupId = "usage-service")
	public void energyUsageEvent(EnergyUsageEvent event) {
		try {
			Point point = Point.measurement(MEASUREMENT_ENERGY_USAGE)
					.addTag(TAG_DEVICE_ID, String.valueOf(event.deviceId()))
					.addField(FIELD_ENERGY_CONSUMED, event.energyConsumed())
					.time(event.timestamp(), WritePrecision.MS);
			influxDBClient.getWriteApiBlocking().writePoint(influxBucket, influxOrg, point);
			log.debug("Stored energy usage in InfluxDB: {}", event);
		} catch (Exception e) {
			log.error("Failed to write energy usage to InfluxDB: {}", event, e);
			throw e; // let Kafka handle retry / DLQ
		}
	}

	// ------------------------------------------------------------------
	// Scheduled aggregation and alerting
	// ------------------------------------------------------------------

	/**
	 * Scheduled task that runs every 10 seconds to aggregate device energy usage
	 * over the past hour, group it by user, and send alerts to Kafka for users
	 * whose aggregated consumption exceeds their configured threshold.
	 */
	@Scheduled(cron = "*/10 * * * * *")
	public void aggregateDeviceEnergyUsage() {
		final Instant now = Instant.now();
		final Instant oneHourAgo = now.minusSeconds(ONE_HOUR_IN_SECONDS);

		final List<DeviceEnergy> deviceEnergies = queryDeviceEnergies(oneHourAgo, now);
		log.debug("Aggregated device energies over the past hour: {}", deviceEnergies);

		enrichWithUserId(deviceEnergies);
		deviceEnergies.removeIf(de -> de.getUserId() == null);

		final Map<Long, List<DeviceEnergy>> userDeviceEnergyMap = deviceEnergies.stream()
				.collect(Collectors.groupingBy(DeviceEnergy::getUserId));
		log.debug("User-Device Energy Map: {}", userDeviceEnergyMap);

		final Map<Long, UserDto> alertingUsers = fetchAlertingUsers(userDeviceEnergyMap.keySet());
		log.debug("Users fetched for alerting: {}", alertingUsers.keySet());

		checkThresholdsAndSendAlerts(userDeviceEnergyMap, alertingUsers);
	}

	/**
	 * Queries InfluxDB for the aggregated energy consumption per device in the
	 * given time range.
	 *
	 * @param from start of the range (inclusive)
	 * @param to   end of the range (exclusive)
	 * @return the parsed {@link DeviceEnergy} entries
	 */
	private List<DeviceEnergy> queryDeviceEnergies(Instant from, Instant to) {
		final String fluxQuery = String.format("""
				from(bucket: "%s")
				  |> range(start: time(v: "%s"), stop: time(v: "%s"))
				  |> filter(fn: (r) => r["_measurement"] == "%s")
				  |> filter(fn: (r) => r["_field"] == "%s")
				  |> group(columns: ["%s"])
				  |> sum(column: "%s")
				""", influxBucket, from, to,
				MEASUREMENT_ENERGY_USAGE, FIELD_ENERGY_CONSUMED, TAG_DEVICE_ID, FLUX_VALUE_COLUMN);

		return extractDeviceEnergies(influxDBClient.getQueryApi().query(fluxQuery, influxOrg));
	}

	/**
	 * Enriches each {@link DeviceEnergy} with the owning user id, calling the
	 * Device Service. Devices whose owner cannot be resolved keep a {@code null}
	 * user id and are filtered out by the caller.
	 *
	 * @param deviceEnergies list mutated in place
	 */
	private void enrichWithUserId(List<DeviceEnergy> deviceEnergies) {
		for (DeviceEnergy deviceEnergy : deviceEnergies) {
			findDevice(deviceEnergy.getDeviceId())
					.ifPresent(device -> deviceEnergy.setUserId(device.userId()));
		}
	}

	/**
	 * Fetches a device from the Device Service, returning an empty optional when the
	 * device does not exist or the remote call fails.
	 *
	 * @param deviceId the device id to resolve
	 * @return the device, or {@link Optional#empty()} if it cannot be resolved
	 */
	private Optional<DeviceDto> findDevice(Long deviceId) {
		try {
			final DeviceDto device = deviceClient.getDeviceById(deviceId);
			if (device == null || device.id() == null) {
				log.warn("Device not found for ID: {}", deviceId);
				return Optional.empty();
			}
			return Optional.of(device);
		} catch (Exception e) {
			log.warn("Failed to fetch device for ID: {}", deviceId, e);
			return Optional.empty();
		}
	}

	/**
	 * Fetches users that have alerting enabled, indexed by user id. Users not
	 * found or with alerting disabled are omitted.
	 *
	 * @param userIds ids to look up
	 * @return a map of alerting-enabled users
	 */
	private Map<Long, UserDto> fetchAlertingUsers(Iterable<Long> userIds) {
		final Map<Long, UserDto> result = new HashMap<>();
		for (Long userId : userIds) {
			findUser(userId).ifPresent(user -> result.put(userId, user));
		}
		return result;
	}

	/**
	 * Fetches a user from the User Service, returning an empty optional when the
	 * user does not exist, has alerting disabled, or the remote call fails.
	 *
	 * @param userId the user id to resolve
	 * @return the alerting-enabled user, or {@link Optional#empty()} otherwise
	 */
	private Optional<UserDto> findUser(Long userId) {
		try {
			final UserDto user = userClient.getUserById(userId);
			if (user == null || user.id() == null) {
				log.warn("User not found for ID: {}", userId);
				return Optional.empty();
			}
			if (!user.alerting()) {
				log.debug("Alerting disabled for user ID: {}", userId);
				return Optional.empty();
			}
			return Optional.of(user);
		} catch (Exception e) {
			log.warn("Failed to fetch user for ID: {}", userId, e);
			return Optional.empty();
		}
	}

	/**
	 * Evaluates the threshold for every alerting-enabled user and publishes an alert
	 * event for each one that has exceeded it.
	 *
	 * @param userDeviceEnergyMap the aggregated consumption grouped by user id
	 * @param alertingUsers       the alerting-enabled users indexed by user id
	 */
	private void checkThresholdsAndSendAlerts(Map<Long, List<DeviceEnergy>> userDeviceEnergyMap,
			Map<Long, UserDto> alertingUsers) {

		for (Map.Entry<Long, UserDto> entry : alertingUsers.entrySet()) {
			checkThresholdAndAlert(entry.getKey(), entry.getValue(), userDeviceEnergyMap.get(entry.getKey()));
		}
	}

	/**
	 * Checks one user's aggregated consumption against their threshold and sends
	 * an {@link AlertingEvent} when exceeded.
	 */
	private void checkThresholdAndAlert(Long userId, UserDto user, List<DeviceEnergy> devices) {
		if (devices == null || devices.isEmpty()) {
			return;
		}

		final double totalConsumption = devices.stream()
				.mapToDouble(DeviceEnergy::getEnergyConsumed)
				.sum();
		final double threshold = user.energyAlertingThreshold();

		if (totalConsumption <= threshold) {
			log.debug("User ID {} is within threshold. consumption={}, threshold={}",
					userId, totalConsumption, threshold);
			return;
		}

		log.info("ALERT: User ID {} exceeded threshold. consumption={}, threshold={}",
				userId, totalConsumption, threshold);

		final AlertingEvent alert = AlertingEvent.builder()
				.userId(userId)
				.message(ALERT_MESSAGE_THRESHOLD_EXCEEDED)
				.threshold(threshold)
				.energyConsumed(totalConsumption)
				.email(user.email())
				.build();
		kafkaTemplate.send(TOPIC_ENERGY_ALERTS, alert);
	}

	// ------------------------------------------------------------------
	// Query aggregated usage for a user
	// ------------------------------------------------------------------

	/**
	 * Retrieves the aggregated energy usage for a specific user over the last
	 * {@code days} days.
	 * <p>
	 * Fetches all devices belonging to the user, queries InfluxDB for the total
	 * energy consumed per device in that period, and returns a {@link UsageDto}
	 * containing the user ID and each device with its aggregated consumption.
	 *
	 * @param userId the user id
	 * @param days   the number of days to look back (must be positive)
	 * @return a {@link UsageDto}; the device list is never {@code null} but may
	 *         be empty if the user has no devices or the query fails
	 */
	public UsageDto getXDaysUsageForUser(Long userId, int days) {
		log.debug("Getting usage for userId {} over past {} days", userId, days);

		final List<DeviceDto> devices = deviceClient.getAllDevicesForUser(userId);
		if (devices == null || devices.isEmpty()) {
			return UsageDto.builder().userId(userId).devices(List.of()).build();
		}

		final Map<Long, Double> aggregated = queryAggregatedConsumptionPerDevice(devices, days, userId);
		final List<DeviceDto> resultDevices = devices.stream()
				.map(dto -> copyWithConsumption(dto, consumptionOf(dto, aggregated)))
				.toList();

		log.debug("Aggregated energy consumption for userId {}: {}", userId, aggregated);
		return UsageDto.builder().userId(userId).devices(resultDevices).build();
	}

	/**
	 * Resolves the aggregated consumption of a device, defaulting to zero when the
	 * device has no id or no aggregated data.
	 *
	 * @param dto         the device
	 * @param aggregated  the aggregated consumption indexed by device id
	 * @return the aggregated consumption of the device
	 */
	private static double consumptionOf(DeviceDto dto, Map<Long, Double> aggregated) {
		return dto.id() == null ? 0.0 : aggregated.getOrDefault(dto.id(), 0.0);
	}

	/**
	 * Copies a device DTO attaching the aggregated energy consumption to it.
	 *
	 * @param dto            the device to copy
	 * @param energyConsumed the aggregated energy consumption to attach
	 * @return a copy of the device with the consumption populated
	 */
	private static DeviceDto copyWithConsumption(DeviceDto dto, double energyConsumed) {
		return DeviceDto.builder()
				.id(dto.id())
				.name(dto.name())
				.type(dto.type())
				.location(dto.location())
				.userId(dto.userId())
				.energyConsumed(energyConsumed)
				.build();
	}

	/**
	 * Queries InfluxDB for the aggregated energy consumption per device over the
	 * last {@code days} days for the given devices.
	 *
	 * @param devices the devices to include in the Flux filter
	 * @param days    the number of days to look back
	 * @param userId  the user id (only used for logging)
	 * @return a map deviceId → aggregated consumption; empty on error
	 */
	private Map<Long, Double> queryAggregatedConsumptionPerDevice(List<DeviceDto> devices, int days, Long userId) {
		final List<String> deviceIds = devices.stream()
				.map(DeviceDto::id)
				.filter(Objects::nonNull)
				.map(String::valueOf)
				.toList();
		if (deviceIds.isEmpty()) {
			return Map.of();
		}

		final Instant now = Instant.now();
		final Instant start = now.minusSeconds((long) days * SECONDS_PER_DAY);
		final String deviceFilter = buildDeviceFilter(deviceIds);

		final String fluxQuery = String.format("""
				from(bucket: "%s")
				  |> range(start: time(v: "%s"), stop: time(v: "%s"))
				  |> filter(fn: (r) => r["_measurement"] == "%s")
				  |> filter(fn: (r) => r["_field"] == "%s")
				  |> filter(fn: (r) => %s)
				  |> group(columns: ["%s"])
				  |> sum(column: "%s")
				""", influxBucket, start, now,
				MEASUREMENT_ENERGY_USAGE, FIELD_ENERGY_CONSUMED, deviceFilter,
				TAG_DEVICE_ID, FLUX_VALUE_COLUMN);

		try {
			return extractDeviceEnergies(influxDBClient.getQueryApi().query(fluxQuery, influxOrg)).stream()
					.collect(Collectors.toMap(
							DeviceEnergy::getDeviceId,
							DeviceEnergy::getEnergyConsumed,
							Double::sum));
		} catch (Exception e) {
			log.error("Failed to query InfluxDB for user {} usage over {} days: {}",
					userId, days, e.getMessage(), e);
			return Map.of();
		}
	}

	/**
	 * Builds the Flux filter expression that restricts the query to the given device
	 * ids, joining the predicates with {@code or}.
	 *
	 * @param deviceIds the device ids to include in the filter
	 * @return the Flux filter predicate expression
	 */
	private static String buildDeviceFilter(List<String> deviceIds) {
		return deviceIds.stream()
				.map(id -> String.format("r[\"%s\"] == \"%s\"", TAG_DEVICE_ID, id))
				.collect(Collectors.joining(" or "));
	}

	// ------------------------------------------------------------------
	// Flux record parsing
	// ------------------------------------------------------------------

	/**
	 * Flattens all {@link FluxTable} records and parses them into
	 * {@link DeviceEnergy} entries. Records with missing or unparsable device ids
	 * are skipped with a warning.
	 */
	private List<DeviceEnergy> extractDeviceEnergies(List<FluxTable> tables) {
		return tables.stream()
				.flatMap(table -> table.getRecords().stream())
				.map(this::parseDeviceEnergy)
				.flatMap(Optional::stream)
				.collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Parses a single Flux record into a {@link DeviceEnergy}, returning an empty
	 * optional when the device id is missing or cannot be parsed.
	 *
	 * @param record the Flux record to parse
	 * @return the parsed device energy, or {@link Optional#empty()} if it is invalid
	 */
	private Optional<DeviceEnergy> parseDeviceEnergy(FluxRecord record) {
		final Object deviceIdObj = record.getValueByKey(TAG_DEVICE_ID);
		if (deviceIdObj == null) {
			return Optional.empty();
		}

		try {
			return Optional.of(DeviceEnergy.builder()
					.deviceId(Long.valueOf(deviceIdObj.toString()))
					.energyConsumed(toDouble(record.getValueByKey(FLUX_VALUE_COLUMN)))
					.build());
		} catch (NumberFormatException e) {
			log.warn("Failed to parse deviceId from flux record: {}", deviceIdObj);
			return Optional.empty();
		}
	}

	/**
	 * Converts a Flux value into a {@code double}, returning zero for non-numeric or
	 * missing values.
	 *
	 * @param value the raw Flux value
	 * @return the value as a double, or {@code 0.0} if it is not a number
	 */
	private static double toDouble(Object value) {
		return value instanceof Number number ? number.doubleValue() : 0.0;
	}
}