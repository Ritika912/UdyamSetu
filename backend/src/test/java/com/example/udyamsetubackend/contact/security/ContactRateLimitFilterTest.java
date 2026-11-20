package com.example.udyamsetubackend.contact.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContactRateLimitFilterTest {

    private final ContactRateLimiter limiter = new ContactRateLimiter(Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"),
            ZoneOffset.UTC));
    private final ContactRateLimitFilter filter = new ContactRateLimitFilter(limiter);

    @Test
    void postsUnderTheLimitPassThrough() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(post("203.0.113.7"), response, chain);

        assertNotNull(chain.getRequest());
        assertEquals(200, response.getStatus());
    }

    @Test
    void sixthPostIsRefusedWith429JsonAndRetryAfter() throws Exception {
        for (int i = 0; i < ContactRateLimiter.MAX_SUBMISSIONS; i++) {
            filter.doFilter(post("203.0.113.7"), new MockHttpServletResponse(), new MockFilterChain());
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(post("203.0.113.7"), response, chain);

        assertEquals(429, response.getStatus());
        assertEquals("600", response.getHeader("Retry-After"));
        assertEquals("application/json", response.getContentType());
        assertTrue(response.getContentAsString().contains("\"status\":429"));
        assertNull(chain.getRequest(), "a refused request must not reach the controller");
    }

    @Test
    void getRequestsAreNotCountedAgainstTheSubmissionLimit() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest get = new MockHttpServletRequest("GET", "/api/contact");
            get.setRemoteAddr("203.0.113.7");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(get, response, new MockFilterChain());
            assertEquals(200, response.getStatus());
        }
    }

    private static MockHttpServletRequest post(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/contact");
        request.setRemoteAddr(remoteAddress);
        return request;
    }
}
