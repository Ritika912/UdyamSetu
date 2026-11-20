package com.example.udyamsetubackend.contact.security;

import com.example.udyamsetubackend.common.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/**
 * Writes filter-level rejections in the shared {@link ErrorResponse} shape. Filters run outside
 * {@code @RestControllerAdvice}, so they need their own writer. The messages are fixed.
 */
final class JsonFilterErrors {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private JsonFilterErrors() {
    }

    static void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        MAPPER.writeValue(response.getOutputStream(), ErrorResponse.of(status, message));
    }
}
