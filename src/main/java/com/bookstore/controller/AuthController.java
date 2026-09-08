package com.bookstore.controller;

import com.bookstore.dto.request.LoginRequest;
import com.bookstore.dto.request.RefreshTokenRequest;
import com.bookstore.dto.request.RegisterRequest;
import com.bookstore.dto.response.ApiResponse;
import com.bookstore.dto.response.AuthResponse;
import com.bookstore.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Account registration and token lifecycle endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new customer account", description = "Creates a customer account after validating the submitted registration details.")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully.", authService.register(request)));
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticates the account and returns access and refresh tokens.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login successful.", authService.login(request)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh authentication tokens", description = "Rotates the submitted refresh token and returns a new authentication response.")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully.", authService.refresh(request)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revokes the submitted refresh token so it can no longer be used for token rotation.")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Parameter(description = "Request containing the refresh token to revoke")
            @Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully.", null));
    }
}
