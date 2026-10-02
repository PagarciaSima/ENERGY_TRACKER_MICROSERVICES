package com.pgs.device.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard error response body returned by the API on client errors.
 */
@Schema(description = "Error response body returned on client errors (4xx)")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    /** The HTTP status code. */
    @Schema(description = "HTTP status code", example = "404")
    private int status;

    /** Short HTTP reason phrase. */
    @Schema(description = "Short HTTP reason phrase", example = "Not Found")
    private String error;

    /** Human-readable message describing the problem. */
    @Schema(description = "Human-readable message describing the problem", example = "User not found")
    private String message;
}
