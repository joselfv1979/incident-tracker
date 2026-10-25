package com.jose.backend.auth;

public record VerificationEmailRequested(String email, String name, String token) {}
