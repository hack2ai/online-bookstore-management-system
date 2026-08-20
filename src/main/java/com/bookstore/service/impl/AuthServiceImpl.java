package com.bookstore.service.impl;

import com.bookstore.dto.request.LoginRequest;
import com.bookstore.dto.request.RegisterRequest;
import com.bookstore.dto.response.AuthResponse;
import com.bookstore.entity.Role;
import com.bookstore.entity.User;
import com.bookstore.exception.DuplicateResourceException;
import com.bookstore.repository.UserRepository;
import com.bookstore.security.CustomUserDetails;
import com.bookstore.security.JwtUtil;
import com.bookstore.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException(
                    "An account with email '" + email + "' already exists.");
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER)
                .phone(normalizeOptional(request.getPhone()))
                .address(normalizeOptional(request.getAddress()))
                .build();

        user = userRepository.save(user);
        log.info("New customer registered: {} (id={})", user.getEmail(), user.getId());

        String token = jwtUtil.generateToken(new CustomUserDetails(user));
        return toAuthResponse(user, token);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);

        log.info("User logged in: {} (role={})",
                userDetails.getUsername(), userDetails.getUser().getRole());

        return toAuthResponse(userDetails.getUser(), token);
    }

    private AuthResponse toAuthResponse(User user, String accessToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
