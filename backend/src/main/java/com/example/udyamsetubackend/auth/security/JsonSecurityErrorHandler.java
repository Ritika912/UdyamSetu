package com.example.udyamsetubackend.auth.security;

import com.example.udyamsetubackend.common.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/**
 * Writes 401 and 403 responses in the same {@link ErrorResponse} shape as the rest of the API.
 *
 * <p>Filter-chain failures never reach {@code @RestControllerAdvice}, so they are handled here. The messages
 * are fixed and never reveal which check failed.
 */
public final class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    static final String UNAUTHENTICATED_MESSAGE = "Authentication is required. Please sign in.";
    static final String FORBIDDEN_MESSAGE = "Access denied. A valid session and security token are required.";

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, UNAUTHENTICATED_MESSAGE);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, HttpStatus.FORBIDDEN, FORBIDDEN_MESSAGE);
    }

    private void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), ErrorResponse.of(status, message));
    }
}
