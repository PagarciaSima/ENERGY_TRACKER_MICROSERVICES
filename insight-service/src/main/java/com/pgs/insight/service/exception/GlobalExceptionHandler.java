package com.pgs.insight.service.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.pgs.insight.service.dto.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for REST controllers.
 * <p>
 * Converts application exceptions into consistent HTTP responses and keeps the
 * API error contract aligned with the responses documented in the OpenAPI
 * specification.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles validation errors raised when request body arguments fail Bean
     * Validation ({@code @Valid} on a {@code @RequestBody}).
     *
     * @param ex the validation exception
     * @return a {@code 400 Bad Request} response containing the field-level
     *         validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Request body validation failed: {}", msg);

        return ResponseEntity.badRequest()
                .body(ErrorResponse.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                        .message(msg)
                        .build());
    }

    /**
     * Handles type mismatch errors raised when a path variable or query
     * parameter cannot be converted to the expected type (e.g.
     * {@code /saving-tips/abc} where a {@code Long} is expected).
     *
     * @param ex the type mismatch exception
     * @return a {@code 400 Bad Request} response describing the offending
     *         parameter
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String msg = String.format("Parameter '%s' must be of type '%s'",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        log.warn("Parameter type mismatch: {}", msg);

        return ResponseEntity.badRequest()
                .body(ErrorResponse.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                        .message(msg)
                        .build());
    }

    /**
     * Handles unexpected exceptions that are not explicitly handled elsewhere.
     *
     * @param ex the unexpected exception
     * @return a {@code 500 Internal Server Error} response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                        .message("Unexpected error")
                        .build());
    }
}