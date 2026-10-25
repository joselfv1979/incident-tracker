package com.jose.backend.auth;

import com.jose.backend.user.Role;
import com.jose.backend.user.User;
import com.jose.backend.user.UserRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final AdminProperties properties;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(
            AdminProperties properties, UserRepository users, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = properties.email();
        String password = properties.password();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.info("ADMIN_EMAIL / ADMIN_PASSWORD not set: skipping initial admin creation");
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(normalizedEmail)) {
            return;
        }

        User admin = new User();
        admin.setName("Administrator");
        admin.setEmail(normalizedEmail);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        admin.setEmailVerified(true);
        users.save(admin);
        log.info("Initial administrator created: {}", normalizedEmail);
    }
}
