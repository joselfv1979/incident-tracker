package com.jose.backend.user;

import java.time.Instant;

public record UserResponse(
        Long id, String name, String email, Role role, boolean emailVerified, Instant createdAt) {

    static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isEmailVerified(),
                user.getCreatedAt());
    }
}
