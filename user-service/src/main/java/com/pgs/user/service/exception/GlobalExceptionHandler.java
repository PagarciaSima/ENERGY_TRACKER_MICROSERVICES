package com.pgs.user.service.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.pgs.user.service.dto.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for the User Service REST controllers.
 * <p>
 * Converts application exceptions into a consistent {@link ErrorResponse} body
 * with the matching HTTP status, and logs the failure so it can be traced.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles cases where the requested user does not exist.
     *
     * @param ex the user not found exception
     * @return a {@code 404 Not Found} response containing the exception message
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(UserNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder()
                        .status(404).error("Not Found").message(ex.getMessage()).build());
    }

    /**
     * Handles attempts to create a user whose email is already registered.
     *
     * @param ex the user already exists exception
     * @return a {@code 409 Conflict} response containing the exception message
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleConflict(UserAlreadyExistsException ex) {
        log.warn("User conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.builder()
                        .status(409).error("Conflict").message(ex.getMessage()).build());
    }

    /**
     * Handles validation errors raised when a request body fails Bean Validation.
     *
     * @param ex the validation exception
     * @return a {@code 400 Bad Request} response listing the invalid fields
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Request validation failed: {}", msg);
        return ResponseEntity.badRequest()
                .body(ErrorResponse.builder()
                        .status(400).error("Bad Request").message(msg).build());
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
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .status(500).error("Internal Server Error").message("Unexpected error").build());
    }
}
