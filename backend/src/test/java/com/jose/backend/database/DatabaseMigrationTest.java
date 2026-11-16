package com.jose.backend.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jose.backend.support.AbstractIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class DatabaseMigrationTest extends AbstractIntegrationTest {

    private static final String INSERT_USER
            = "INSERT INTO users (name, email, password_hash, role) VALUES (?, ?, 'hash', ?)";

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void allMigrationsApplyOnAnEmptyDatabase() {
        Integer failed = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = false", Integer.class);
        Integer applied = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version IS NOT NULL", Integer.class);

        assertThat(failed).isZero();
        assertThat(applied).isGreaterThanOrEqualTo(5);

        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class);
        assertThat(tables)
                .contains(
                        "users",
                        "incidents",
                        "comments",
                        "incident_history",
                        "email_verification_tokens");
    }

    @Test
    void theDatabaseRejectsAnUnknownRole() {
        assertThatThrownBy(() -> jdbc.update(INSERT_USER, "Bad Role", "bad.role@example.com", "SUPERUSER"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void emailsAreUnique() {
        jdbc.update(INSERT_USER, "First", "same@example.com", "USER");

        assertThatThrownBy(() -> jdbc.update(INSERT_USER, "Second", "same@example.com", "USER"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
