package com.jose.backend.auth;

import com.jose.backend.user.User;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    Optional<EmailVerificationToken> findFirstByUserOrderByCreatedAtDesc(User user);

    @Modifying(flushAutomatically = true)
    @Query("update EmailVerificationToken t set t.usedAt = :now where t.user = :user and t.usedAt is null")
    int invalidateActiveTokens(@Param("user") User user, @Param("now") Instant now);
}
