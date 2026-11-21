package com.jose.backend.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Login, registration and email verification")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    @Operation(
            summary = "Create an account",
            description = "Creates a USER account and sends a verification email. The response is "
            + "the same whether or not the email already has an account.")
    @SecurityRequirements
    @ResponseStatus(HttpStatus.ACCEPTED)
    @ApiResponse(
        responseCode = "429",
        description = "Too many requests. The Retry-After header says how many seconds to wait")
    public MessageResponse register(@Valid @RequestBody RegisterRequest request) {
        registrationService.register(request);
        return new MessageResponse("If the data is valid, a verification email has been sent");
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify an email address with the token sent by email")
    @SecurityRequirements
    @ApiResponse(responseCode = "400", description = "The token is invalid or has expired")
    @ApiResponse(
        responseCode = "429",
        description = "Too many requests. The Retry-After header says how many seconds to wait")
    public MessageResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        registrationService.verify(request.token());
        return new MessageResponse("Email verified");
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
            summary = "Send a new verification email",
            description = "At most one email per user every 60 seconds. The response never "
            + "reveals whether the account exists.")
    @SecurityRequirements
    @ApiResponse(
        responseCode = "429",
        description = "Too many requests. The Retry-After header says how many seconds to wait")
    public MessageResponse resend(@Valid @RequestBody ResendVerificationRequest request) {
        registrationService.resend(request.email());
        return new MessageResponse("If the account exists and is not verified, a new email has been sent");
    }
}
