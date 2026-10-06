package com.pgs.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pgs.dto.UsageDto;
import com.pgs.usage.service.service.UsageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

/**
 * REST controller that exposes endpoints to query aggregated energy usage.
 * <p>
 * The base path for all endpoints in this controller is {@code /api/v1/usage}.
 * It provides operations to retrieve the total energy consumed by a user's devices
 * over a specified period.
 */
@Tag(name = "Usage", description = "Endpoints to query aggregated energy usage")
@RestController
@RequestMapping("/api/v1/usage")
@Validated
public class UsageController {

    private final UsageService usageService;

    /**
     * Constructs a new {@code UsageController} with the given {@link UsageService}.
     *
     * @param usageService the service used to retrieve usage data
     */
    public UsageController(UsageService usageService) {
        this.usageService = usageService;
    }

    /**
     * Retrieves the aggregated energy usage for a specific user over the last {@code days} days.
     * <p>
     * The response includes the user ID and a list of devices with their aggregated consumption.
     *
     * @param userId the ID of the user whose usage is to be retrieved; must be positive
     * @param days   the number of days to aggregate; defaults to 3, must be between 1 and 365
     * @return a {@link ResponseEntity} containing the {@link UsageDto} with the aggregated usage
     */
    @Operation(
            summary = "Get aggregated energy usage for a user",
            description = "Returns the total energy consumed by all devices of a user over the last N days.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usage retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UsageDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<UsageDto> getUserDeviceUsage(
            @Parameter(description = "User id", example = "1")
            @PathVariable @Positive Long userId,

            @Parameter(description = "Number of days to aggregate", example = "3")
            @RequestParam(defaultValue = "3") @Min(1) @Max(365) int days) {

        final UsageDto usage = usageService.getXDaysUsageForUser(userId, days);
        return ResponseEntity.ok(usage);
    }
}