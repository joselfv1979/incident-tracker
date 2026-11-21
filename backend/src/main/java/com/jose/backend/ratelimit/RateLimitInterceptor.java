package com.jose.backend.ratelimit;

import io.micrometer.core.instrument.Counter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/** Counts the request against its address and refuses it when the limit is reached. */
class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter limiter;
    private final Counter rejected;

    RateLimitInterceptor(RateLimiter limiter, Counter rejected) {
        this.limiter = limiter;
        this.rejected = rejected;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        RateLimiter.Decision decision = limiter.tryAcquire(ClientAddress.keyOf(request));
        if (!decision.allowed()) {
            rejected.increment();
            // The exception handler turns it into a 429 response
            throw new TooManyRequestsException(decision.retryAfterSeconds());
        }
        return true;
    }
}
