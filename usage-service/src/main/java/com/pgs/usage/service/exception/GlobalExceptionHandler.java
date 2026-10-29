package com.pgs.usage.service.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import com.pgs.usage.service.dto.ErrorResponse;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for the Usage Service REST controllers.
 * <p>
 * Converts validation and unexpected exceptions into a consistent
 * {@link ErrorResponse} body with the matching HTTP status, and logs the failure
 * so it can be traced.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles constraint violations raised when a request parameter fails Bean
     * Validation (for example a {@code @Positive} path variable).
     *
     * @param ex the constraint violation exception
     * @return a {@code 400 Bad Request} response listing the invalid parameters
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
        log.warn("Constraint violation: {}", msg);
        return build(HttpStatus.BAD_REQUEST, msg);
    }

    /**
     * Handles method validation errors raised by Spring when a handler method
     * argument is invalid.
     *
     * @param ex the method validation exception
     * @return a {@code 400 Bad Request} response listing the validation messages
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException ex) {
        String msg = ex.getAllErrors().stream()
                .map(e -> e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Method validation failed: {}", msg);
        return build(HttpStatus.BAD_REQUEST, msg);
    }

    /**
     * Handles validation errors raised when a request body fails Bean Validation.
     *
     * @param ex the validation exception
     * @return a {@code 400 Bad Request} response listing the invalid fields
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Request body validation failed: {}", msg);
        return build(HttpStatus.BAD_REQUEST, msg);
    }

    /**
     * Handles unexpected exceptions that are not explicitly handled elsewhere.
     *
     * @param ex the unexpected exception
     * @return a {@code 500 Internal Server Error} response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error while processing the request", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }

    /**
     * Builds the shared {@link ErrorResponse} body for the given status.
     *
     * @param status  the HTTP status to return
     * @param message the message to include in the response body
     * @return the response entity with the error contract body
     */
    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.builder()
                        .status(status.value())
                        .error(status.getReasonPhrase())
                        .message(message)
                        .build());
    }
}