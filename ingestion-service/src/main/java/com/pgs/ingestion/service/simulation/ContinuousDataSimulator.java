package com.pgs.ingestion.service.simulation;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.pgs.ingestion.service.dto.EnergyUsageDto;

import lombok.extern.slf4j.Slf4j;

/**
 * Load simulator that posts energy usage readings to the ingestion endpoint.
 * <p>
 * It is a {@link CommandLineRunner}: on startup it logs that the simulator is
 * enabled. The actual burst of requests is produced by
 * {@link #sendMockData()} (currently triggered manually, as the scheduling
 * annotation is commented out).
 */
@Slf4j
@Component
public class ContinuousDataSimulator implements CommandLineRunner {

    /** HTTP client used to post the simulated readings to the ingestion endpoint. */
    private final RestClient restClient = RestClient.create();

    /** Random source used to generate the simulated readings. */
    private final Random random = new Random();

    @Value("${simulation.requests-per-interval}")
    private int requestsPerInterval;

    @Value("${simulation.endpoint}")
    private String ingestionEndpoint;


    /**
     * Callback executed once the application context has started.
     *
     * @param args command-line arguments passed to the application
     * @throws Exception if the runner fails during startup
     */
    @Override
    public void run(String... args) throws Exception {
        log.info("ContinuousDataSimulator started...");
    }


    /**
     * Sends a burst of {@code simulation.requests-per-interval} random readings to
     * the ingestion endpoint, logging the outcome of each request.
     */
    //@Scheduled(fixedRateString = "${simulation.interval-ms}")
    public void sendMockData() {
        for (int i = 0; i < requestsPerInterval; i++) {
            EnergyUsageDto dto = EnergyUsageDto.builder()
                    .deviceId(random.nextLong(1, 6))
                    .energyConsumed(Math.round(random.nextDouble(0.0, 2.0) * 100.0) / 100.0)
                    .timestamp(LocalDateTime.now()
                            .atZone(ZoneId.systemDefault()).toInstant())
                    .build();

            try {
                restClient.post()
                        .uri(ingestionEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(dto)
                        .retrieve()
                        .toBodilessEntity();

                log.info ("Sent mock data: {}", dto);
            } catch (Exception e) {
                log.error("Failed to send data: {}", e.getMessage());
            }
        }
    }
}