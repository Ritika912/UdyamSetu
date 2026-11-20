package com.example.udyamsetubackend.auth.service;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * In-memory login throttle: {@value #MAX_FAILED_ATTEMPTS} failed attempts within
 * {@value #FAILURE_WINDOW_MINUTES} minutes lock the key for {@value #LOCK_MINUTES} minutes.
 *
 * <p>Each attempt is a two-step operation: {@link #tryBeginAttempt} reserves a slot, and then either
 * {@link #recordFailure} or {@link #recordSuccess} completes it. The reservation counts in-flight attempts
 * as well as recorded failures. So concurrent requests cannot all pass the check before any failure is
 * recorded: at most {@value #MAX_FAILED_ATTEMPTS} attempts can be outstanding or failed in one window. Every
 * method is synchronized, so the check and the update are atomic.
 *
 * <p>Storage is bounded to {@value #MAX_TRACKED_KEYS} keys, and expired entries are removed on every call. The
 * state is per process, which suits the single-instance deployment.
 */
@Service
public class LoginAttemptService {

    static final int MAX_FAILED_ATTEMPTS = 5;
    static final long FAILURE_WINDOW_MINUTES = 15;
    static final long LOCK_MINUTES = 15;
    static final int MAX_TRACKED_KEYS = 1_000;

    private static final Duration FAILURE_WINDOW = Duration.ofMinutes(FAILURE_WINDOW_MINUTES);
    private static final Duration LOCK_DURATION = Duration.ofMinutes(LOCK_MINUTES);

    /**
     * @param failures    failed attempts counted in the current window
     * @param inFlight    attempts that have started and not yet finished
     * @param windowStart when the window began
     * @param lockedUntil end of the lock, or null when not locked
     */
    private record Attempts(int failures, int inFlight, Instant windowStart, Instant lockedUntil) {

        Attempts withInFlight(int count) {
            return new Attempts(failures, count, windowStart, lockedUntil);
        }
    }

    private final Clock clock;
    private final Map<String, Attempts> attempts = new HashMap<>();

    public LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    /**
     * Atomically checks the lock and reserves a slot for one attempt.
     *
     * @return true if the attempt may proceed, which must then be completed with {@link #recordFailure} or
     *         {@link #recordSuccess}; false if the key is locked or the outstanding limit is reached
     */
    public synchronized boolean tryBeginAttempt(String key) {
        Instant now = clock.instant();
        purgeExpired(now);
        Attempts current = attempts.get(key);
        if (current == null) {
            attempts.put(key, new Attempts(0, 1, now, null));
            enforceBound();
            return true;
        }
        if (current.lockedUntil() != null) {
            return false;
        }
        if (current.failures() + current.inFlight() >= MAX_FAILED_ATTEMPTS) {
            return false;
        }
        attempts.put(key, current.withInFlight(current.inFlight() + 1));
        return true;
    }

    /** Completes an attempt that failed. The {@value #MAX_FAILED_ATTEMPTS}th failure in the window starts a lock. */
    public synchronized void recordFailure(String key) {
        Instant now = clock.instant();
        purgeExpired(now);
        Attempts current = attempts.get(key);
        Attempts updated;
        if (current == null) {
            updated = new Attempts(1, 0, now, null);
        } else {
            int failures = current.failures() + 1;
            int inFlight = Math.max(0, current.inFlight() - 1);
            Instant lockedUntil = current.lockedUntil();
            if (lockedUntil == null && failures >= MAX_FAILED_ATTEMPTS) {
                lockedUntil = now.plus(LOCK_DURATION);
            }
            updated = new Attempts(failures, inFlight, current.windowStart(), lockedUntil);
        }
        attempts.put(key, updated);
        enforceBound();
    }

    /** Completes an attempt that succeeded. The counter for the key is cleared. */
    public synchronized void recordSuccess(String key) {
        attempts.remove(key);
    }

    /** Returns true while the key is locked. */
    public synchronized boolean isLocked(String key) {
        purgeExpired(clock.instant());
        Attempts current = attempts.get(key);
        return current != null && current.lockedUntil() != null;
    }

    /** Number of keys currently tracked. Visible to tests only. */
    synchronized int trackedKeyCount() {
        return attempts.size();
    }

    /** An entry expires when its lock ends, or when its failure window ends without a lock. */
    private void purgeExpired(Instant now) {
        attempts.entrySet().removeIf(entry -> isExpired(entry.getValue(), now));
    }

    private static boolean isExpired(Attempts entry, Instant now) {
        if (entry.lockedUntil() != null) {
            return !now.isBefore(entry.lockedUntil());
        }
        return !now.isBefore(entry.windowStart().plus(FAILURE_WINDOW));
    }

    /** Evicts the entries whose windows started earliest, so storage can never grow without limit. */
    private void enforceBound() {
        while (attempts.size() > MAX_TRACKED_KEYS) {
            attempts.entrySet().stream()
                    .min(Comparator.comparing((Map.Entry<String, Attempts> entry) -> entry.getValue().windowStart()))
                    .map(Map.Entry::getKey)
                    .ifPresent(attempts::remove);
        }
    }
}
