package com.bookstore.controller;

import com.bookstore.dto.request.LoginRequest;
import com.bookstore.dto.request.RefreshTokenRequest;
import com.bookstore.dto.request.RegisterRequest;
import com.bookstore.dto.response.ApiResponse;
import com.bookstore.dto.response.AuthResponse;
import com.bookstore.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Account and token lifecycle endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new customer account")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully.", authService.register(request)));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive access and refresh tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login successful.", authService.login(request)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate a refresh token and issue a new access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully.", authService.refresh(request)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke a refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully.", null));
    }
}
