package com.example.udyamsetubackend.contact.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Limits {@code POST /api/contact} per client address, before any body is parsed or email is sent.
 *
 * <p>The client address is {@code getRemoteAddr()}. Behind Nginx in production the forward-headers strategy
 * resolves it from {@code X-Forwarded-For}, and Nginx must overwrite that header.
 */
public final class ContactRateLimitFilter extends OncePerRequestFilter {

    static final String CONTACT_METHOD = "POST";
    static final String TOO_MANY_MESSAGE = "Too many messages were sent from this connection. Please wait before trying again.";

    private final ContactRateLimiter rateLimiter;

    public ContactRateLimitFilter(ContactRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!CONTACT_METHOD.equals(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        Duration wait = rateLimiter.tryAcquire(request.getRemoteAddr());
        if (wait.isZero()) {
            chain.doFilter(request, response);
            return;
        }
        // Round up, so the client never retries before the window has actually reopened.
        long seconds = Math.max(1, (wait.toMillis() + 999) / 1000);
        response.setHeader("Retry-After", String.valueOf(seconds));
        JsonFilterErrors.write(response, HttpStatus.TOO_MANY_REQUESTS, TOO_MANY_MESSAGE);
    }
}
