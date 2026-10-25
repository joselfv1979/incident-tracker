package com.jose.backend.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties(prefix = "app.verification")
public record VerificationProperties(Duration tokenTtl, Duration resendCooldown) {}
