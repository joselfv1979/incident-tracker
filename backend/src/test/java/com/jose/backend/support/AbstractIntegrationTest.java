package com.jose.backend.support;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for the tests that need a database.
 *
 * <p>
 * A single PostgreSQL container is started once per test run and shared by
 * every test class and Spring context. Testcontainers removes it when the JVM
 * ends.
 */
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    // Keep it aligned with the image used in docker-compose.yml
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
