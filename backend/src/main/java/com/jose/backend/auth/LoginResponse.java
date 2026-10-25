package com.jose.backend.auth;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {}
