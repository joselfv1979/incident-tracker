package com.jose.backend.auth;

import com.jose.backend.user.Role;
import com.jose.backend.user.User;
import com.jose.backend.user.UserRepository;
import java.time.Instant;
import java.util.Locale;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final UserRepository users;
    private final EmailVerificationTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final VerificationProperties properties;

    public RegistrationService(
            UserRepository users,
            EmailVerificationTokenRepository tokens,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher events,
            VerificationProperties properties) {
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
        this.properties = properties;
    }

    @Transactional
    public void register(RegisterRequest request) {
        String email = normalize(request.email());
        User user = users.findByEmail(email).orElse(null);

        if (user != null) {
            // Never reveal that the account exists. An unverified account only gets
            // a fresh email once per cooldown period.
            if (user.isEmailVerified() || withinCooldown(user)) {
                return;
            }
        } else {
            user = new User();
            user.setEmail(email);
            user.setRole(Role.USER);
        }

        user.setName(request.name().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        users.save(user);
        issueToken(user);
    }

    @Transactional
    public void verify(String rawToken) {
        EmailVerificationToken token = tokens.findByTokenHash(VerificationTokens.hash(rawToken.trim()))
                .orElseThrow(InvalidVerificationTokenException::new);
        User user = token.getUser();

        if (user.isEmailVerified()) {
            return; // the link was opened twice
        }
        Instant now = Instant.now();
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(now)) {
            throw new InvalidVerificationTokenException();
        }

        token.setUsedAt(now);
        user.setEmailVerified(true);
    }

    @Transactional
    public void resend(String rawEmail) {
        users.findByEmail(normalize(rawEmail))
                .filter(user -> !user.isEmailVerified())
                .filter(user -> !withinCooldown(user))
                .ifPresent(this::issueToken);
    }

    private boolean withinCooldown(User user) {
        return tokens.findFirstByUserOrderByCreatedAtDesc(user)
                .map(last -> last.getCreatedAt().plus(properties.resendCooldown()).isAfter(Instant.now()))
                .orElse(false);
    }

    private void issueToken(User user) {
        Instant now = Instant.now();
        tokens.invalidateActiveTokens(user, now);

        String rawToken = VerificationTokens.generate();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(VerificationTokens.hash(rawToken));
        token.setExpiresAt(now.plus(properties.tokenTtl()));
        tokens.save(token);

        events.publishEvent(new VerificationEmailRequested(user.getEmail(), user.getName(), rawToken));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
