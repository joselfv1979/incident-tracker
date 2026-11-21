package com.jose.backend.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sliding window limiter: at most {@code limit} hits per key in any window of the given
 * length. It keeps the time of each recent hit, so memory per key is bounded by the limit.
 */
public class RateLimiter {

    public record Decision(boolean allowed, long retryAfterSeconds) {
        static Decision permitted() {
            return new Decision(true, 0);
        }

        static Decision denied(long retryAfterSeconds) {
            return new Decision(false, retryAfterSeconds);
        }
    }

    private static final long CLEANUP_INTERVAL_MILLIS = 60_000;

    private final int limit;
    private final long windowMillis;
    private final Clock clock;
    private final ConcurrentHashMap<String, Deque<Long>> hits = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanup;

    public RateLimiter(int limit, Duration window, Clock clock) {
        if (limit < 1) {
            throw new IllegalArgumentException("The limit must be at least 1");
        }
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("The window must be positive");
        }
        this.limit = limit;
        this.windowMillis = window.toMillis();
        this.clock = clock;
        this.lastCleanup = new AtomicLong(clock.millis());
    }

    /** Counts the hit if there is room. Otherwise says how long to wait. */
    public Decision tryAcquire(String key) {
        return evaluate(key, true);
    }

    /** The same decision as {@link #tryAcquire}, without counting anything. */
    public Decision peek(String key) {
        return evaluate(key, false);
    }

    /** Counts a hit no matter what. When the window is full, the oldest hit is dropped. */
    public void record(String key) {
        cleanupIfDue();
        long now = clock.millis();
        hits.compute(key, (k, existing) -> {
            Deque<Long> deque = existing != null ? existing : new ArrayDeque<>();
            evictExpired(deque, now);
            if (deque.size() >= limit) {
                deque.pollFirst();
            }
            deque.addLast(now);
            return deque;
        });
    }

    public void reset(String key) {
        hits.remove(key);
    }

    int trackedKeys() {
        return hits.size();
    }

    private Decision evaluate(String key, boolean count) {
        cleanupIfDue();
        long now = clock.millis();
        Decision[] result = new Decision[1];
        // compute() is atomic per key, so concurrent requests cannot both take the last slot
        hits.compute(key, (k, existing) -> {
            Deque<Long> deque = existing != null ? existing : new ArrayDeque<>();
            evictExpired(deque, now);
            if (deque.size() >= limit) {
                result[0] = Decision.denied(secondsUntilFree(deque, now));
            } else {
                result[0] = Decision.permitted();
                if (count) {
                    deque.addLast(now);
                }
            }
            // Returning null removes the entry, so keys that are no longer used do not pile up
            return deque.isEmpty() ? null : deque;
        });
        return result[0];
    }

    private long secondsUntilFree(Deque<Long> deque, long now) {
        long waitMillis = deque.peekFirst() + windowMillis - now;
        return Math.max(1, (waitMillis + 999) / 1000);
    }

    private void evictExpired(Deque<Long> deque, long now) {
        while (!deque.isEmpty() && now - deque.peekFirst() >= windowMillis) {
            deque.pollFirst();
        }
    }

    // Keys that are never seen again would stay forever: sweep them once a minute
    private void cleanupIfDue() {
        long now = clock.millis();
        long last = lastCleanup.get();
        if (now - last < CLEANUP_INTERVAL_MILLIS || !lastCleanup.compareAndSet(last, now)) {
            return;
        }
        for (String key : hits.keySet()) {
            hits.computeIfPresent(key, (k, deque) -> {
                evictExpired(deque, now);
                return deque.isEmpty() ? null : deque;
            });
        }
    }
}
