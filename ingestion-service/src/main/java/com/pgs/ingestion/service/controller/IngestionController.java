package com.pgs.ingestion.service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pgs.ingestion.service.dto.EnergyUsageDto;
import com.pgs.ingestion.service.dto.ErrorResponse;      // ← CORREGIDO: tu DTO
import com.pgs.ingestion.service.service.IngestionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for ingesting energy usage data.
 * <p>
 * Receives energy readings from IoT devices and publishes them to Kafka for
 * further processing by downstream services (Usage, Insight, Alert).
 * <p>
 * All endpoints are exposed under {@code /api/v1/ingestion}.
 */
@Tag(name = "Ingestion", description = "Endpoints to ingest energy usage data from IoT devices")
@RestController
@RequestMapping("/api/v1/ingestion")
@RequiredArgsConstructor
public class IngestionController {

    private final IngestionService ingestionService;

    /**
     * Ingests a single energy usage reading.
     * <p>
     * The reading is validated, enriched with a timestamp if missing, and published
     * to Kafka. This endpoint is designed to be called by IoT devices and returns
     * immediately after the message is queued.
     *
     * @param usageDto the energy usage data to ingest
     * @return HTTP 201 if the reading was accepted
     */
    @Operation(
            summary = "Ingest an energy usage reading",
            description = "Receives a single energy usage reading from an IoT device, "
                    + "validates it, and publishes it to Kafka for downstream processing. "
                    + "Returns 201 as soon as the message is queued.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EnergyUsageDto.class),
                            examples = @ExampleObject(
                                    name = "ingestEnergyUsageExample",
                                    summary = "Request body to ingest a reading",
                                    value = """
                                            {
                                              "deviceId": 42,
                                              "energyConsumed": 3.5,
                                              "timestamp": "2026-10-04T15:30:45Z"
                                            }
                                            """))))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reading accepted and queued"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request body",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "badRequest",
                                    summary = "Example error response",
                                    value = "{\"status\": 400, \"error\": \"Bad Request\", "
                                            + "\"message\": \"deviceId: must not be null\"}"))),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<Void> ingestData(@Valid @RequestBody EnergyUsageDto usageDto) {
        ingestionService.ingestEnergyUsage(usageDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}