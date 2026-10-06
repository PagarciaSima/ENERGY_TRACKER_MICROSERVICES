package com.pgs.ingestion.service.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.pgs.ingestion.service.dto.EnergyUsageDto;
import com.pgs.kafka.event.EnergyUsageEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service that ingests energy usage readings from IoT devices and publishes
 * them to Kafka for downstream processing.
 * <p>
 * This service is intentionally thin: it does not persist data, does not
 * process readings, and does not know who consumes the events. It only
 * converts the incoming DTO into a domain event and pushes it to the
 * {@code energy-usage} topic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    /** Name of the Kafka topic where energy usage events are published. */
    private static final String TOPIC_ENERGY_USAGE = "energy-usage";

    private final KafkaTemplate<String, EnergyUsageEvent> kafkaTemplate;

    /**
     * Ingests a single energy usage reading and publishes it to Kafka.
     * <p>
     * The reading is converted into an {@link EnergyUsageEvent} and sent to
     * the {@code energy-usage} topic. The method returns as soon as the
     * message is handed off to Kafka; downstream processing happens
     * asynchronously.
     *
     * @param input the energy usage data received from the device
     */
    public void ingestEnergyUsage(EnergyUsageDto input) {
        EnergyUsageEvent event = toEvent(input);

        kafkaTemplate.send(TOPIC_ENERGY_USAGE, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish energy usage event for device {}: {}",
                                event.deviceId(), ex.getMessage(), ex);
                    } else {
                        log.debug("Published energy usage event for device {} to topic '{}' (partition={}, offset={})",
                                event.deviceId(),
                                TOPIC_ENERGY_USAGE,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    /**
     * Maps an {@link EnergyUsageDto} to an {@link EnergyUsageEvent}.
     *
     * @param input the DTO received from the API
     * @return the event to be published to Kafka
     */
    private EnergyUsageEvent toEvent(EnergyUsageDto input) {
        return EnergyUsageEvent.builder()
                .deviceId(input.deviceId())
                .energyConsumed(input.energyConsumed())
                .timestamp(input.timestamp())
                .build();
    }
}