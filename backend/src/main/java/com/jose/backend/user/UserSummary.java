package com.jose.backend.user;

/** Minimal data to pick an assignee: no email, no account details. */
public record UserSummary(Long id, String name, Role role) {

    public static UserSummary from(User user) {
        return new UserSummary(user.getId(), user.getName(), user.getRole());
    }
}
