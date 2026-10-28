package com.jose.backend.user;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public record CurrentUser(Long id, Role role) {

    /** Agents and administrators work on incidents; users only report them. */
    public boolean isStaff() {
        return role == Role.AGENT || role == Role.ADMIN;
    }

    public static CurrentUser from(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        Role role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> Role.valueOf(authority.substring("ROLE_".length())))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("No role"));
        return new CurrentUser(Long.valueOf(jwt.getSubject()), role);
    }
}
