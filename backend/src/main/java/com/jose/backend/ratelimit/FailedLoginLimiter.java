package com.jose.backend.ratelimit;

import io.micrometer.core.instrument.Counter;

/** Locks an account for a while after too many failed logins, wherever they come from. */
public class FailedLoginLimiter {

    // Both are null when rate limiting is turned off
    private final RateLimiter limiter;
    private final Counter rejected;

    public FailedLoginLimiter(RateLimiter limiter, Counter rejected) {
        this.limiter = limiter;
        this.rejected = rejected;
    }

    /** Refuses the attempt, even with the right password, while the account is locked. */
    public void checkAllowed(String email) {
        if (limiter == null) {
            return;
        }
        RateLimiter.Decision decision = limiter.peek(email);
        if (!decision.allowed()) {
            rejected.increment();
            throw new TooManyRequestsException(decision.retryAfterSeconds());
        }
    }

    public void recordFailure(String email) {
        if (limiter != null) {
            limiter.record(email);
        }
    }

    public void reset(String email) {
        if (limiter != null) {
            limiter.reset(email);
        }
    }
}
