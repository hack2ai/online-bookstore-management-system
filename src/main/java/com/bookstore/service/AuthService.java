package com.bookstore.service;

import com.bookstore.dto.request.LoginRequest;
import com.bookstore.dto.request.RegisterRequest;
import com.bookstore.dto.response.AuthResponse;

public interface AuthService {

    /**
     * Register a new customer account.
     * Throws {@code DuplicateResourceException} if the email is already taken.
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticate an existing user and return a signed JWT.
     * Throws {@code BadCredentialsException} (Spring Security) if credentials are wrong.
     */
    AuthResponse login(LoginRequest request);
}
