package com.pgs.insight.service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pgs.insight.service.dto.ErrorResponse;
import com.pgs.insight.service.dto.InsightDto;
import com.pgs.insight.service.service.InsightService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * REST controller exposing energy insight endpoints.
 * <p>
 * Aggregates usage data retrieved from the Usage Service and forwards it to an
 * Ollama chat model to produce personalized recommendations for a given user.
 * <p>
 * All endpoints are exposed under {@code /api/v1/insight}.
 */
@Tag(name = "Insight", description = "Endpoints to generate energy insights and saving tips for users")
@RestController
@RequestMapping("/api/v1/insight")
@RequiredArgsConstructor
public class InsightController {

    private final InsightService insightService;

    /**
     * Generates personalized energy-saving tips for the given user.
     * <p>
     * The service aggregates the last three days of usage data and asks the
     * chat model how to reduce consumption and how it compares to average
     * households.
     *
     * @param userId identifier of the user
     * @return the generated tips together with the aggregated energy usage
     */
    @Operation(
            summary = "Get personalized energy saving tips",
            description = "Aggregates the last 3 days of energy usage for the given user and "
                    + "asks the chat model for tailored recommendations on how to reduce "
                    + "consumption and how it compares to average households.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Insight generated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = InsightDto.class),
                            examples = @ExampleObject(
                                    name = "savingTipsExample",
                                    summary = "Example saving tips response",
                                    value = """
                                            {
                                              "userId": 42,
                                              "tips": "Consider shifting heavy loads to off-peak hours and unplugging idle devices.",
                                              "energyUsage": 12.75
                                            }
                                            """))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid or missing userId",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "badRequest",
                                    summary = "Example error response",
                                    value = "{\"status\": 400, \"error\": \"Bad Request\", "
                                            + "\"message\": \"userId: must be a valid number\"}"))),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "503",
                    description = "The Usage Service or the AI model (Ollama) is not reachable",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "dependencyUnavailable",
                                    summary = "Example unavailable dependency response",
                                    value = "{\"status\": 503, \"error\": \"Service Unavailable\", "
                                            + "\"message\": \"Ollama is unavailable: Connection refused\"}")))
    })
    @GetMapping("/saving-tips/{userId}")
    public ResponseEntity<InsightDto> getSavingTips(
            @Parameter(description = "Identifier of the user", required = true, example = "42")
            @PathVariable Long userId) {
        final InsightDto insight = insightService.getSavingsTips(userId);
        return ResponseEntity.ok(insight);
    }

    /**
     * Builds a general overview of the user's energy usage.
     * <p>
     * The service aggregates the last three days of usage data and asks the
     * chat model for a concise analysis with actionable insights.
     *
     * @param userId identifier of the user
     * @return the generated overview together with the aggregated energy usage
     */
    @Operation(
            summary = "Get an overview of the user's energy usage",
            description = "Aggregates the last 3 days of energy usage for the given user and "
                    + "asks the chat model for a concise analysis with actionable insights.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Overview generated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = InsightDto.class),
                            examples = @ExampleObject(
                                    name = "overviewExample",
                                    summary = "Example overview response",
                                    value = """
                                            {
                                              "userId": 42,
                                              "tips": "Your consumption is in line with similar households. The HVAC system accounts for most of the load; consider a smart thermostat.",
                                              "energyUsage": 12.75
                                            }
                                            """))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid or missing userId",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "badRequest",
                                    summary = "Example error response",
                                    value = "{\"status\": 400, \"error\": \"Bad Request\", "
                                            + "\"message\": \"userId: must be a valid number\"}"))),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "503",
                    description = "The Usage Service or the AI model (Ollama) is not reachable",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "dependencyUnavailable",
                                    summary = "Example unavailable dependency response",
                                    value = "{\"status\": 503, \"error\": \"Service Unavailable\", "
                                            + "\"message\": \"Ollama is unavailable: Connection refused\"}")))
    })
    @GetMapping("/overview/{userId}")
    public ResponseEntity<InsightDto> getOverview(
            @Parameter(description = "Identifier of the user", required = true, example = "42")
            @PathVariable Long userId) {
        final InsightDto insight = insightService.getOverview(userId);
        return ResponseEntity.ok(insight);
    }
}