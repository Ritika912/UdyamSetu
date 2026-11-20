package com.example.udyamsetubackend.common.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Uniform error body returned by the API.
 *
 * @param timestamp   when the error was produced
 * @param status      HTTP status code
 * @param error       HTTP reason phrase
 * @param message     human-readable summary
 * @param fieldErrors per-field validation messages; empty when not applicable
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {

    /** Builds the standard body for an error without field-level details. */
    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, Map.of());
    }
}
