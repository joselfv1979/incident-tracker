package com.jose.backend.ratelimit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig implements WebMvcConfigurer {

    private final RateLimitProperties properties;
    private final MeterRegistry meters;
    private final Clock clock = Clock.systemUTC();

    public RateLimitConfig(RateLimitProperties properties, MeterRegistry meters) {
        this.properties = properties;
        this.meters = meters;
    }

    @Bean
    FailedLoginLimiter failedLoginLimiter() {
        if (!properties.enabled()) {
            return new FailedLoginLimiter(null, null);
        }
        return new FailedLoginLimiter(limiter(properties.failedLogin()), rejectedCounter("failed-login"));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (!properties.enabled()) {
            return;
        }
        limit(registry, "/api/auth/login", properties.login(), "login");
        limit(registry, "/api/auth/register", properties.register(), "register");
        limit(registry, "/api/auth/resend-verification", properties.resend(), "resend");
        limit(registry, "/api/auth/verify-email", properties.verify(), "verify");
    }

    // Each endpoint gets its own limiter, so using one does not use up the others
    private void limit(
            InterceptorRegistry registry, String path, RateLimitProperties.Rule rule, String name) {
        registry.addInterceptor(new RateLimitInterceptor(limiter(rule), rejectedCounter(name)))
                .addPathPatterns(path);
    }

    private RateLimiter limiter(RateLimitProperties.Rule rule) {
        return new RateLimiter(rule.limit(), rule.window(), clock);
    }

    // One counter per rule, with the rule as a tag: no addresses or emails are recorded
    private Counter rejectedCounter(String rule) {
        return Counter.builder("rate_limit.rejected")
                .description("Requests refused by the rate limiter")
                .tag("rule", rule)
                .register(meters);
    }
}
