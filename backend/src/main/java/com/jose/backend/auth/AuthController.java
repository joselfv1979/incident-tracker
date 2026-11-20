package com.jose.backend.auth;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jose.backend.user.User;
import com.jose.backend.user.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Login, registration and email verification")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current user")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.getById(Long.valueOf(jwt.getSubject()));
        return new MeResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }

    @PostMapping("/login")
    @Operation(summary = "Log in", description = "Returns a JWT to send as a bearer token.")
    @SecurityRequirements
    @ApiResponse(responseCode = "401", description = "Wrong email or password")
    @ApiResponse(responseCode = "403", description = "The email address is not verified yet")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

}
