package com.jose.backend.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jose.backend.support.AbstractIntegrationTest;
import com.jose.backend.user.Role;
import com.jose.backend.user.User;
import com.jose.backend.user.UserRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.rate-limit.enabled=true",
    "app.rate-limit.login.limit=3",
    "app.rate-limit.login.window=10m",
    "app.rate-limit.register.limit=2",
    "app.rate-limit.register.window=10m",
    "app.rate-limit.failed-login.limit=3",
    "app.rate-limit.failed-login.window=10m"
})
class RateLimitIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "secret-password";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean JavaMailSender mailSender;
    @Autowired MeterRegistry meters;

    private double rejected(String rule) {
        Counter counter = meters.find("rate_limit.rejected").tag("rule", rule).counter();
        return counter == null ? 0 : counter.count();
    }

    // These tests are not transactional (the counters live in the application),
    // so they remove what they create
    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM users WHERE email LIKE 'ratelimit.%@example.com'");
    }

    private void createVerifiedUser(String email) {
        User user = new User();
        user.setName("Rate limit user");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(Role.USER);
        user.setEmailVerified(true);
        users.save(user);
    }

    private ResultActions login(String address, String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .with(request -> {
                    request.setRemoteAddr(address);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}""".formatted(email, password)));
    }

    private ResultActions register(String address, String email) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .with(request -> {
                    request.setRemoteAddr(address);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Someone","email":"%s","password":"%s"}""".formatted(email, PASSWORD)));
    }

    @Test
    void loginAttemptsAreLimitedPerAddress() throws Exception {
        double before = rejected("login");
        String address = "10.10.0.1";
        for (int i = 0; i < 3; i++) {
            login(address, "nobody." + i + "@example.com", "whatever-password")
                    .andExpect(status().isUnauthorized());
        }

        MvcResult blocked = login(address, "nobody.3@example.com", "whatever-password")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andReturn();

        long retryAfter = Long.parseLong(blocked.getResponse().getHeader("Retry-After"));
        assertThat(retryAfter).isBetween(1L, 600L);
        assertThat(rejected("login")).isEqualTo(before + 1);
    }

    @Test
    void otherAddressesAreNotAffected() throws Exception {
        for (int i = 0; i < 3; i++) {
            login("10.10.0.2", "nobody." + i + "@example.com", "whatever-password");
        }
        login("10.10.0.2", "nobody.9@example.com", "whatever-password")
                .andExpect(status().isTooManyRequests());

        login("10.10.0.3", "nobody.9@example.com", "whatever-password")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationsAreLimitedPerAddress() throws Exception {
        String address = "10.10.0.4";

        register(address, "ratelimit.reg.1@example.com").andExpect(status().isAccepted());
        register(address, "ratelimit.reg.2@example.com").andExpect(status().isAccepted());
        register(address, "ratelimit.reg.3@example.com")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void anAccountIsLockedAfterTooManyFailuresEvenFromOtherAddresses() throws Exception {
        createVerifiedUser("ratelimit.locked@example.com");
        double before = rejected("failed-login");

        // Three wrong passwords, each from a different address
        for (int i = 1; i <= 3; i++) {
            login("10.20.0." + i, "ratelimit.locked@example.com", "wrong-password")
                    .andExpect(status().isUnauthorized());
        }

        // The right password is refused too while the account is locked
        login("10.20.0.4", "ratelimit.locked@example.com", PASSWORD)
                .andExpect(status().isTooManyRequests());

        assertThat(rejected("failed-login")).isEqualTo(before + 1);
    }

    @Test
    void aSuccessfulLoginClearsTheFailures() throws Exception {
        createVerifiedUser("ratelimit.cleared@example.com");

        login("10.30.0.1", "ratelimit.cleared@example.com", "wrong-password")
                .andExpect(status().isUnauthorized());
        login("10.30.0.2", "ratelimit.cleared@example.com", "wrong-password")
                .andExpect(status().isUnauthorized());
        login("10.30.0.3", "ratelimit.cleared@example.com", PASSWORD).andExpect(status().isOk());

        // Four failures in total would have locked the account without the reset
        login("10.30.0.4", "ratelimit.cleared@example.com", "wrong-password")
                .andExpect(status().isUnauthorized());
        login("10.30.0.5", "ratelimit.cleared@example.com", "wrong-password")
                .andExpect(status().isUnauthorized());
    }
}
