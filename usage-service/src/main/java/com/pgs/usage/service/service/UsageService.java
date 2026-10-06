package com.pgs.usage.service.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
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
import com.pgs.usage.service.model.Device;
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
	private static final long ONE_HOUR_IN_SECONDS = 3600L;
	private static final long SECONDS_PER_DAY = 24L * 3600L;

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
	 * "energy-usage" topic.
	 * <p>
	 * It writes the energy consumption data point to InfluxDB. If the write fails,
	 * the error is logged and re-thrown so that Kafka can handle the retry / DLQ
	 * policy.
	 *
	 * @param energyUsageEvent the energy usage event containing device ID, energy
	 *                         consumed, and timestamp
	 */
	@KafkaListener(topics = TOPIC_ENERGY_USAGE, groupId = "usage-service")
	public void energyUsageEvent(EnergyUsageEvent energyUsageEvent) {
		try {
			Point point = Point.measurement(MEASUREMENT_ENERGY_USAGE)
					.addTag("deviceId", String.valueOf(energyUsageEvent.deviceId()))
					.addField(FIELD_ENERGY_CONSUMED, energyUsageEvent.energyConsumed())
					.time(energyUsageEvent.timestamp(), WritePrecision.MS);
			influxDBClient.getWriteApiBlocking().writePoint(influxBucket, influxOrg, point);
			log.debug("Stored energy usage in InfluxDB: {}", energyUsageEvent);
		} catch (Exception e) {
			log.error("Failed to write energy usage to InfluxDB: {}", energyUsageEvent, e);
			throw e; // let Kafka handle retry / DLQ
		}
	}

	// ------------------------------------------------------------------
	// Scheduled aggregation and alerting
	// ------------------------------------------------------------------

	/**
	 * Scheduled task that runs every 10 seconds to aggregate device energy usage
	 * over the past hour.
	 * <p>
	 * It queries InfluxDB for the sum of energy consumed per device, enriches the
	 * data with user information from external services, aggregates per user,
	 * checks against user-defined thresholds, and sends alert events to Kafka if
	 * thresholds are exceeded.
	 */
	@Scheduled(cron = "*/10 * * * * *")
	public void aggregateDeviceEnergyUsage() {
		final Instant now = Instant.now();
		final Instant oneHourAgo = now.minusSeconds(ONE_HOUR_IN_SECONDS);

		final List<DeviceEnergy> deviceEnergies = queryDeviceEnergiesOverLastHour(oneHourAgo, now);
		log.debug("Aggregated device energies over the past hour: {}", deviceEnergies);

		enrichWithUserId(deviceEnergies);

		// drop devices whose user id could not be resolved
		deviceEnergies.removeIf(de -> de.getUserId() == null);

		final Map<Long, List<DeviceEnergy>> userDeviceEnergyMap = deviceEnergies.stream()
				.collect(Collectors.groupingBy(DeviceEnergy::getUserId));
		log.debug("User-Device Energy Map: {}", userDeviceEnergyMap);

		final Map<Long, UserDto> usersById = fetchUsers(userDeviceEnergyMap.keySet());
		log.debug("Users fetched for alerting: {}", usersById.keySet());

		checkThresholdsAndSendAlerts(userDeviceEnergyMap, usersById);
	}

	/**
	 * Queries InfluxDB for the aggregated energy consumption per device in the
	 * given time range.
	 *
	 * @param from the start of the time range (inclusive)
	 * @param to   the end of the time range (exclusive)
	 * @return the list of {@link DeviceEnergy} with device id and aggregated
	 *         consumption
	 */
	private List<DeviceEnergy> queryDeviceEnergiesOverLastHour(Instant from, Instant to) {
		final String fluxQuery = String.format("""
				from(bucket: "%s")
				  |> range(start: time(v: "%s"), stop: time(v: "%s"))
				  |> filter(fn: (r) => r["_measurement"] == "%s")
				  |> filter(fn: (r) => r["_field"] == "%s")
				  |> group(columns: ["deviceId"])
				  |> sum(column: "_value")
				""", influxBucket, from.toString(), to.toString(), MEASUREMENT_ENERGY_USAGE, FIELD_ENERGY_CONSUMED);

		final QueryApi queryApi = influxDBClient.getQueryApi();
		final List<FluxTable> tables = queryApi.query(fluxQuery, influxOrg);

		final List<DeviceEnergy> result = new ArrayList<>();
		for (FluxTable table : tables) {
			for (FluxRecord record : table.getRecords()) {
				final Object deviceIdObj = record.getValueByKey("deviceId");
				if (deviceIdObj == null) {
					continue;
				}
				final Object value = record.getValueByKey("_value");
				final double energyConsumed = value instanceof Number ? ((Number) value).doubleValue() : 0.0;

				try {
					result.add(DeviceEnergy.builder().deviceId(Long.valueOf(deviceIdObj.toString()))
							.energyConsumed(energyConsumed).build());
				} catch (NumberFormatException nfe) {
					log.warn("Failed to parse deviceId from flux record: {}", deviceIdObj);
				}
			}
		}
		return result;
	}

	/**
	 * Enriches each {@link DeviceEnergy} with the owning user id, by calling the
	 * Device Service. Devices whose owner could not be resolved are left with a
	 * {@code null} user id and filtered out by the caller.
	 *
	 * @param deviceEnergies the list to enrich (mutated in place)
	 */
	private void enrichWithUserId(List<DeviceEnergy> deviceEnergies) {
		for (DeviceEnergy deviceEnergy : deviceEnergies) {
			try {
				final DeviceDto device = deviceClient.getDeviceById(deviceEnergy.getDeviceId());
				if (device == null || device.id() == null) {
					log.warn("Device not found for ID: {}", deviceEnergy.getDeviceId());
					continue;
				}
				deviceEnergy.setUserId(device.userId());
			} catch (Exception e) {
				log.warn("Failed to fetch device for ID: {}", deviceEnergy.getDeviceId(), e);
			}
		}
	}

	/**
	 * Fetches the users that have alerting enabled, indexed by user id.
	 *
	 * @param userIds the ids to look up
	 * @return a map with the users that have alerting enabled; users not found or
	 *         with alerting disabled are omitted
	 */
	private Map<Long, UserDto> fetchUsers(Iterable<Long> userIds) {
		final Map<Long, UserDto> result = new HashMap<>();
		for (Long userId : userIds) {
			try {
				final UserDto user = userClient.getUserById(userId);
				if (user == null || user.id() == null) {
					log.warn("User not found for ID: {}", userId);
					continue;
				}
				if (!user.alerting()) {
					log.debug("Alerting disabled for user ID: {}", userId);
					continue;
				}
				result.put(userId, user);
			} catch (Exception e) {
				log.warn("Failed to fetch user for ID: {}", userId, e);
			}
		}
		return result;
	}

	/**
	 * Checks each user's aggregated consumption against their configured threshold
	 * and sends an {@link AlertingEvent} to Kafka when it is exceeded.
	 *
	 * @param userDeviceEnergyMap the aggregated consumption grouped by user id
	 * @param usersById           the users that have alerting enabled
	 */
	private void checkThresholdsAndSendAlerts(Map<Long, List<DeviceEnergy>> userDeviceEnergyMap,
			Map<Long, UserDto> usersById) {

		for (Map.Entry<Long, UserDto> entry : usersById.entrySet()) {
			final Long userId = entry.getKey();
			final UserDto user = entry.getValue();

			final List<DeviceEnergy> devices = userDeviceEnergyMap.get(userId);
			if (devices == null || devices.isEmpty()) {
				continue;
			}

			final double totalConsumption = devices.stream().mapToDouble(DeviceEnergy::getEnergyConsumed).sum();
			final double threshold = user.energyAlertingThreshold();

			if (totalConsumption > threshold) {
				log.info("ALERT: User ID {} exceeded the energy threshold. " + "Total consumption: {}, threshold: {}",
						userId, totalConsumption, threshold);

				final AlertingEvent alertingEvent = AlertingEvent.builder().userId(userId)
						.message("Energy consumption threshold exceeded").threshold(threshold)
						.energyConsumed(totalConsumption).email(user.email()).build();
				kafkaTemplate.send(TOPIC_ENERGY_ALERTS, alertingEvent);
			} else {
				log.debug("User ID {} is within the energy threshold. " + "Total consumption: {}, threshold: {}",
						userId, totalConsumption, threshold);
			}
		}
	}

	// ------------------------------------------------------------------
	// Query aggregated usage for a user
	// ------------------------------------------------------------------

	/**
	 * Retrieves the aggregated energy usage for a specific user over the last
	 * {@code days} days.
	 * <p>
	 * It fetches all devices belonging to the user, queries InfluxDB for the total
	 * energy consumed by each device within the given period, and returns a
	 * {@link UsageDto} containing the user ID and a list of devices with their
	 * aggregated consumption.
	 *
	 * @param userId the ID of the user
	 * @param days   the number of days to look back (must be positive)
	 * @return a {@link UsageDto} with the aggregated usage; the device list is
	 *         never {@code null} but may be empty if the user has no devices or the
	 *         query fails
	 */
	public UsageDto getXDaysUsageForUser(Long userId, int days) {
		log.debug("Getting usage for userId {} over past {} days", userId, days);

		final List<DeviceDto> devicesDto = deviceClient.getAllDevicesForUser(userId);

		if (devicesDto == null || devicesDto.isEmpty()) {
			return UsageDto.builder().userId(userId).devices(List.of()).build();
		}

		final List<Device> devices = devicesDto.stream().map(dto -> Device.builder().id(dto.id()).name(dto.name())
				.type(dto.type()).location(dto.location()).userId(dto.userId()).build()).toList();

		final Map<Long, Double> aggregatedMap = queryAggregatedConsumptionPerDevice(devices, days, userId);

		final List<DeviceDto> resultDevices = devices.stream().map(device -> {
			final double consumed = device.getId() == null ? 0.0 : aggregatedMap.getOrDefault(device.getId(), 0.0);
			return DeviceDto.builder().id(device.getId()).name(device.getName()).type(device.getType())
					.location(device.getLocation()).userId(device.getUserId()).energyConsumed(consumed).build();
		}).toList();

		log.debug("Aggregated energy consumption for userId {}: {}", userId, aggregatedMap);

		return UsageDto.builder().userId(userId).devices(resultDevices).build();
	}

	/**
	 * Queries InfluxDB for the aggregated energy consumption per device over the
	 * last {@code days} days for the given devices.
	 *
	 * @param devices the devices to query; their ids are used to build the Flux
	 *                filter
	 * @param days    the number of days to look back
	 * @param userId  the user id (only used for logging)
	 * @return a map deviceId - aggregated consumption; empty on error
	 */
	private Map<Long, Double> queryAggregatedConsumptionPerDevice(List<Device> devices, int days, Long userId) {

		final List<String> deviceIdStrings = devices.stream().map(Device::getId).filter(Objects::nonNull)
				.map(String::valueOf).toList();

		if (deviceIdStrings.isEmpty()) {
			return Map.of();
		}

		final Instant now = Instant.now();
		final Instant start = now.minusSeconds((long) days * SECONDS_PER_DAY);

		final String deviceFilter = deviceIdStrings.stream()
				.map(idStr -> String.format("r[\"deviceId\"] == \"%s\"", idStr)).collect(Collectors.joining(" or "));

		final String fluxQuery = String.format("""
				from(bucket: "%s")
				  |> range(start: time(v: "%s"), stop: time(v: "%s"))
				  |> filter(fn: (r) => r["_measurement"] == "%s")
				  |> filter(fn: (r) => r["_field"] == "%s")
				  |> filter(fn: (r) => %s)
				  |> group(columns: ["deviceId"])
				  |> sum(column: "_value")
				""", influxBucket, start.toString(), now.toString(), MEASUREMENT_ENERGY_USAGE, FIELD_ENERGY_CONSUMED,
				deviceFilter);

		final Map<Long, Double> aggregatedMap = new HashMap<>();

		try {
			final QueryApi queryApi = influxDBClient.getQueryApi();
			final List<FluxTable> tables = queryApi.query(fluxQuery, influxOrg);

			for (FluxTable table : tables) {
				for (FluxRecord record : table.getRecords()) {
					final Object deviceIdObj = record.getValueByKey("deviceId");
					if (deviceIdObj == null) {
						continue;
					}

					final Object value = record.getValueByKey("_value");
					final double energyConsumed = value instanceof Number ? ((Number) value).doubleValue() : 0.0;

					try {
						final Long deviceId = Long.valueOf(deviceIdObj.toString());
						aggregatedMap.merge(deviceId, energyConsumed, Double::sum);
					} catch (NumberFormatException nfe) {
						log.warn("Failed to parse deviceId from flux record: {}", deviceIdObj);
					}
				}
			}
		} catch (Exception e) {
			log.error("Failed to query InfluxDB for user {} usage over {} days: {}", userId, days, e.getMessage(), e);
			return Map.of();
		}

		return aggregatedMap;
	}
}