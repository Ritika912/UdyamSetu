package com.example.udyamsetubackend.contact.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Database-free tests for the per-client contact limit. Time is controlled through a mutable clock. */
class ContactRateLimiterTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-20T10:00:00Z"));
    private final ContactRateLimiter limiter = new ContactRateLimiter(clock);

    @Test
    void fifthSubmissionIsAllowedAndTheSixthIsRefused() {
        for (int i = 0; i < ContactRateLimiter.MAX_SUBMISSIONS; i++) {
            assertEquals(Duration.ZERO, limiter.tryAcquire("203.0.113.7"), "submission " + (i + 1));
        }

        Duration wait = limiter.tryAcquire("203.0.113.7");

        assertTrue(!wait.isZero() && !wait.isNegative());
        assertEquals(ContactRateLimiter.WINDOW, wait, "all five arrived at the same instant, so the full window applies");
    }

    @Test
    void retryDelayReflectsTheOldestSubmissionInTheWindow() {
        for (int i = 0; i < 5; i++) {
            limiter.tryAcquire("203.0.113.7");
            clock.advance(Duration.ofMinutes(1));
        }
        // Now is 10:05. The oldest submission was at 10:00, so the window reopens at 10:10.
        assertEquals(Duration.ofMinutes(5), limiter.tryAcquire("203.0.113.7"));
    }

    @Test
    void clientIsAllowedAgainOnceTheWindowHasPassed() {
        for (int i = 0; i < 5; i++) {
            limiter.tryAcquire("203.0.113.7");
        }
        clock.advance(ContactRateLimiter.WINDOW);

        assertEquals(Duration.ZERO, limiter.tryAcquire("203.0.113.7"));
    }

    @Test
    void windowSlidesRatherThanResettingAsAWhole() {
        for (int i = 0; i < 5; i++) {
            limiter.tryAcquire("203.0.113.7");
            clock.advance(Duration.ofMinutes(2));
        }
        // 10:10 now. Only the 10:00 submission has left the window, so exactly one more is allowed.
        assertEquals(Duration.ZERO, limiter.tryAcquire("203.0.113.7"));
        assertTrue(!limiter.tryAcquire("203.0.113.7").isZero());
    }

    @Test
    void clientsAreLimitedIndependently() {
        for (int i = 0; i < 5; i++) {
            limiter.tryAcquire("203.0.113.7");
        }

        assertEquals(Duration.ZERO, limiter.tryAcquire("198.51.100.9"));
    }

    @Test
    void storageStaysWithinTheBoundWhenManyClientsArrive() {
        for (int i = 0; i < ContactRateLimiter.MAX_TRACKED_CLIENTS + 500; i++) {
            limiter.tryAcquire("client-" + i);
            clock.advance(Duration.ofMillis(1));
        }

        assertTrue(limiter.trackedClientCount() <= ContactRateLimiter.MAX_TRACKED_CLIENTS);
    }

    /** A clock whose time is moved by the test. */
    static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
