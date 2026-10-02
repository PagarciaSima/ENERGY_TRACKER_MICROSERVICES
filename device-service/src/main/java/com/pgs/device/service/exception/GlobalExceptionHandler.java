package com.pgs.device.service.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.pgs.device.service.dto.ErrorResponse;

/**
 * Global exception handler for REST controllers.
 * <p>
 * Converts application exceptions into appropriate HTTP responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles validation errors raised when request arguments fail Bean Validation.
     *
     * @param ex the validation exception
     * @return a {@code 400 Bad Request} response containing the validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.badRequest()
                .body(ErrorResponse.builder()
                        .status(400)
                        .error("Bad Request")
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
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .status(500)
                        .error("Internal Server Error")
                        .message("Unexpected error")
                        .build());
    }

    /**
     * Handles cases where the requested device cannot be found.
     *
     * @param ex the device not found exception
     * @return a {@code 404 Not Found} response containing the exception message
     */
    @ExceptionHandler(DeviceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(DeviceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder()
                        .status(404)
                        .error("Not Found")
                        .message(ex.getMessage())
                        .build());
    }
}
