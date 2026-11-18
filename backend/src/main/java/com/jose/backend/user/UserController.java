package com.jose.backend.user;

import com.jose.backend.common.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Accounts and roles")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List users (administrators)")
    @ApiResponse(responseCode = "403", description = "Only administrators")
    public PageResponse<UserResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return userService.list(page, size);
    }

    @GetMapping("/assignable")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(
            summary = "List the users an incident can be assigned to (agents and administrators)",
            description = "Minimal data: id, name and role.")
    public List<UserSummary> assignable() {
        return userService.assignable();
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Change the role of a user (administrators)",
            description = "Administrators cannot change their own role.")
    @ApiResponse(responseCode = "403", description = "Not an administrator, or changing your own role")
    @ApiResponse(responseCode = "404", description = "User not found")
    public UserResponse changeRole(
            @PathVariable Long id,
            @Valid @RequestBody ChangeRoleRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return userService.changeRole(id, request.role(), Long.valueOf(jwt.getSubject()));
    }
}
