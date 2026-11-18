package com.jose.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Incident Tracker API",
                version = "1.0",
                description = "REST API to register, assign, track and resolve incidents.",
                license = @License(name = "MIT", url = "https://opensource.org/license/mit")),
        // Everything requires a token unless an operation says otherwise
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Log in with POST /api/auth/login and paste the accessToken here.")
public class OpenApiConfig {
}
