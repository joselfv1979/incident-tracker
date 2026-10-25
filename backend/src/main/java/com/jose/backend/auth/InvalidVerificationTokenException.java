package com.jose.backend.auth;

public class InvalidVerificationTokenException extends RuntimeException {
    public InvalidVerificationTokenException() {
        super("Invalid or expired verification token");
    }
}
