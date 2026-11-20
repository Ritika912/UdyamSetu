package com.example.udyamsetubackend.contact.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * In-memory sliding-window limit on contact submissions: {@value #MAX_SUBMISSIONS} per client within
 * {@link #WINDOW}. Each client's timestamps are kept, so the limit is exact and never resets early.
 *
 * <p>Every method is synchronized, so the check and the update are atomic. Storage is bounded to
 * {@value #MAX_TRACKED_CLIENTS} clients. When it is full, the least recently active client is evicted. The state
 * is per process, which suits the single-instance deployment.
 */
@Component
public class ContactRateLimiter {

    public static final int MAX_SUBMISSIONS = 5;
    static final Duration WINDOW = Duration.ofMinutes(10);
    static final int MAX_TRACKED_CLIENTS = 10_000;

    private final Clock clock;
    private final Map<String, Deque<Instant>> submissions = new HashMap<>();

    public ContactRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * Records an attempt if the client is under the limit.
     *
     * @return {@link Duration#ZERO} when the attempt is allowed; otherwise how long until the next attempt is allowed
     */
    public synchronized Duration tryAcquire(String clientKey) {
        Instant now = clock.instant();
        purgeExpired(now);
        Deque<Instant> recent = submissions.computeIfAbsent(clientKey, key -> new ArrayDeque<>());
        while (!recent.isEmpty() && !now.isBefore(recent.peekFirst().plus(WINDOW))) {
            recent.pollFirst();
        }
        if (recent.size() >= MAX_SUBMISSIONS) {
            return Duration.between(now, recent.peekFirst().plus(WINDOW));
        }
        recent.addLast(now);
        enforceBound();
        return Duration.ZERO;
    }

    /** Number of clients currently tracked. Visible to tests only. */
    synchronized int trackedClientCount() {
        return submissions.size();
    }

    /** A client's entry is dropped once its newest submission has left the window. */
    private void purgeExpired(Instant now) {
        submissions.values().removeIf(deque -> deque.isEmpty()
                || !now.isBefore(deque.peekLast().plus(WINDOW)));
    }

    /** Evicts the client whose newest submission is oldest, so storage can never grow without limit. */
    private void enforceBound() {
        while (submissions.size() > MAX_TRACKED_CLIENTS) {
            submissions.entrySet().stream()
                    .min(Comparator.comparing(entry -> entry.getValue().peekLast()))
                    .map(Map.Entry::getKey)
                    .ifPresent(submissions::remove);
        }
    }
}
