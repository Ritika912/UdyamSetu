package com.example.udyamsetubackend.auth.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Database-free unit tests for login throttling. Time is controlled through an injected clock. */
class LoginAttemptServiceTest {

    private static final String KEY = "owner";
    private static final Instant START = Instant.parse("2026-11-20T10:00:00Z");

    private final MutableClock clock = new MutableClock(START);
    private final LoginAttemptService service = new LoginAttemptService(clock);

    @Test
    void fourFailuresDoNotLock() {
        recordFailures(KEY, 4);

        assertFalse(service.isLocked(KEY));
    }

    @Test
    void fifthFailureLocksForFifteenMinutes() {
        recordFailures(KEY, 5);

        assertTrue(service.isLocked(KEY));

        clock.advance(Duration.ofMinutes(15).minusSeconds(1));
        assertTrue(service.isLocked(KEY), "still locked one second before the 15-minute lock ends");

        clock.advance(Duration.ofSeconds(1));
        assertFalse(service.isLocked(KEY), "unlocked exactly when the 15-minute lock ends");
    }

    @Test
    void failuresOutsideTheWindowDoNotAccumulate() {
        recordFailures(KEY, 4);

        clock.advance(Duration.ofMinutes(15));
        service.recordFailure(KEY);

        assertFalse(service.isLocked(KEY), "the window restarted, so this is the first failure of a new window");
    }

    @Test
    void failuresWithinTheWindowAccumulate() {
        recordFailures(KEY, 4);

        clock.advance(Duration.ofMinutes(14));
        service.recordFailure(KEY);

        assertTrue(service.isLocked(KEY), "the fifth failure 14 minutes after the first is inside the window");
    }

    @Test
    void successClearsTheCounter() {
        recordFailures(KEY, 4);

        service.recordSuccess(KEY);
        recordFailures(KEY, 4);

        assertFalse(service.isLocked(KEY));
    }

    @Test
    void keysAreTrackedIndependently() {
        recordFailures("owner", 5);

        assertTrue(service.isLocked("owner"));
        assertFalse(service.isLocked("someone-else"));
    }

    @Test
    void expiredEntriesAreRemovedOnNextCall() {
        service.recordFailure(KEY);
        assertEquals(1, service.trackedKeyCount());

        clock.advance(Duration.ofMinutes(16));
        service.isLocked("another-key");

        assertEquals(0, service.trackedKeyCount(), "an entry whose window has ended is cleaned up");
    }

    @Test
    void storageIsBounded() {
        for (int i = 0; i < LoginAttemptService.MAX_TRACKED_KEYS + 500; i++) {
            service.recordFailure("attacker-" + i);
        }

        assertTrue(service.trackedKeyCount() <= LoginAttemptService.MAX_TRACKED_KEYS,
                "tracked keys must never exceed the bound");
    }

    @Test
    void reservationsCountTowardsTheLimitBeforeAnyFailureIsRecorded() {
        for (int i = 0; i < LoginAttemptService.MAX_FAILED_ATTEMPTS; i++) {
            assertTrue(service.tryBeginAttempt(KEY), "attempt " + (i + 1) + " must be allowed");
        }

        assertFalse(service.tryBeginAttempt(KEY), "a sixth attempt must be refused while five are outstanding");
    }

    @Test
    void completedFailuresKeepCountingAgainstTheLimit() {
        for (int i = 0; i < LoginAttemptService.MAX_FAILED_ATTEMPTS; i++) {
            assertTrue(service.tryBeginAttempt(KEY));
            service.recordFailure(KEY);
        }

        assertTrue(service.isLocked(KEY));
        assertFalse(service.tryBeginAttempt(KEY));
    }

    @Test
    void successfulAttemptClearsTheCounterSoTheNextWindowStartsClean() {
        assertTrue(service.tryBeginAttempt(KEY));
        service.recordSuccess(KEY);

        for (int i = 0; i < LoginAttemptService.MAX_FAILED_ATTEMPTS; i++) {
            assertTrue(service.tryBeginAttempt(KEY), "after a success, a full new set of attempts is allowed");
            service.recordFailure(KEY);
        }
        assertTrue(service.isLocked(KEY));
    }

    @Test
    void concurrentAttemptsNeverExceedTheLimit() throws Exception {
        int threads = 64;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger allowed = new AtomicInteger();
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    if (service.tryBeginAttempt(KEY)) {
                        allowed.incrementAndGet();
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(LoginAttemptService.MAX_FAILED_ATTEMPTS, allowed.get(),
                "exactly the configured number of attempts may start, however many threads race");
    }

    @Test
    void concurrentFailuresLockAtExactlyTheConfiguredCount() throws Exception {
        int threads = 32;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger allowed = new AtomicInteger();
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    if (service.tryBeginAttempt(KEY)) {
                        allowed.incrementAndGet();
                        service.recordFailure(KEY);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(LoginAttemptService.MAX_FAILED_ATTEMPTS, allowed.get());
        assertTrue(service.isLocked(KEY));
    }

    private void recordFailures(String key, int count) {
        for (int i = 0; i < count; i++) {
            service.recordFailure(key);
        }
    }

    /** A clock whose time only moves when a test advances it. */
    static final class MutableClock extends Clock {

        private Instant now;
        private final ZoneId zone;

        MutableClock(Instant start) {
            this(start, ZoneOffset.UTC);
        }

        private MutableClock(Instant start, ZoneId zone) {
            this.now = start;
            this.zone = zone;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId newZone) {
            return new MutableClock(now, newZone);
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
