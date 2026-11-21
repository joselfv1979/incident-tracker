package com.jose.backend.ratelimit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        boolean enabled, Rule login, Rule register, Rule resend, Rule verify, Rule failedLogin) {

    public record Rule(int limit, Duration window) {}
}
