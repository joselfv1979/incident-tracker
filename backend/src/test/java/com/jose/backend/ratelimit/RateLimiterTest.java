package com.jose.backend.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-20T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();

    @Test
    void allowsUpToTheLimitAndThenBlocks() {
        RateLimiter limiter = new RateLimiter(3, Duration.ofMinutes(1), clock);

        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryAcquire("a").allowed()).isTrue();
        }
        RateLimiter.Decision denied = limiter.tryAcquire("a");

        assertThat(denied.allowed()).isFalse();
        assertThat(denied.retryAfterSeconds()).isEqualTo(60);
    }

    @Test
    void theWindowSlidesInsteadOfResettingAllAtOnce() {
        RateLimiter limiter = new RateLimiter(2, Duration.ofSeconds(60), clock);

        assertThat(limiter.tryAcquire("a").allowed()).isTrue(); // t = 0
        clock.advance(Duration.ofSeconds(30));
        assertThat(limiter.tryAcquire("a").allowed()).isTrue(); // t = 30

        RateLimiter.Decision denied = limiter.tryAcquire("a");
        assertThat(denied.allowed()).isFalse();
        assertThat(denied.retryAfterSeconds()).isEqualTo(30); // the first hit expires at t = 60

        clock.advance(Duration.ofSeconds(30)); // t = 60: only the first hit has expired
        assertThat(limiter.tryAcquire("a").allowed()).isTrue();
        assertThat(limiter.tryAcquire("a").allowed()).isFalse();
    }

    @Test
    void keysDoNotAffectEachOther() {
        RateLimiter limiter = new RateLimiter(1, Duration.ofMinutes(1), clock);

        assertThat(limiter.tryAcquire("a").allowed()).isTrue();
        assertThat(limiter.tryAcquire("a").allowed()).isFalse();
        assertThat(limiter.tryAcquire("b").allowed()).isTrue();
    }

    @Test
    void peekDoesNotCountAnything() {
        RateLimiter limiter = new RateLimiter(1, Duration.ofMinutes(1), clock);

        assertThat(limiter.peek("a").allowed()).isTrue();
        assertThat(limiter.peek("a").allowed()).isTrue();
        assertThat(limiter.tryAcquire("a").allowed()).isTrue();
        assertThat(limiter.peek("a").allowed()).isFalse();
    }

    @Test
    void recordCountsAndResetForgets() {
        RateLimiter limiter = new RateLimiter(2, Duration.ofMinutes(1), clock);

        limiter.record("a");
        limiter.record("a");
        assertThat(limiter.peek("a").allowed()).isFalse();

        limiter.reset("a");
        assertThat(limiter.peek("a").allowed()).isTrue();
    }

    @Test
    void recordingBeyondTheLimitDoesNotGrowTheMemoryOrExtendTheBlock() {
        RateLimiter limiter = new RateLimiter(2, Duration.ofSeconds(60), clock);

        for (int i = 0; i < 50; i++) {
            limiter.record("a");
        }
        clock.advance(Duration.ofSeconds(60));

        assertThat(limiter.peek("a").allowed()).isTrue();
    }

    @Test
    void keysThatAreNoLongerUsedAreForgotten() {
        RateLimiter limiter = new RateLimiter(1, Duration.ofSeconds(30), clock);
        for (int i = 0; i < 100; i++) {
            limiter.tryAcquire("key-" + i);
        }
        assertThat(limiter.trackedKeys()).isEqualTo(100);

        clock.advance(Duration.ofMinutes(2));
        limiter.tryAcquire("new-key"); // the sweep runs on the next use

        assertThat(limiter.trackedKeys()).isEqualTo(1);
    }

    @Test
    void invalidConfigurationIsRejected() {
        assertThatThrownBy(() -> new RateLimiter(0, Duration.ofMinutes(1), clock))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RateLimiter(1, Duration.ZERO, clock))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
