package com.bookstore.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String RAW_SECRET = "12345678901234567890123456789012";
    private static final String SECRET = Base64.getEncoder().encodeToString(RAW_SECRET.getBytes(StandardCharsets.UTF_8));
    private static final String ISSUER = "test-bookstore";

    private final JwtUtil jwtUtil = new JwtUtil(SECRET, 900_000L, ISSUER);

    @Test
    void generatedTokenContainsExpectedSubjectAndValidates() {
        UserDetails user = User.withUsername("customer@example.com")
                .password("encoded")
                .roles("CUSTOMER")
                .build();

        String token = jwtUtil.generateToken(user);

        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("customer@example.com");
        assertThat(jwtUtil.isTokenValid(token, user)).isTrue();
        assertThat(jwtUtil.isTokenExpired(token)).isFalse();
    }

    @Test
    void tokenWithDifferentIssuerIsRejected() {
        SecretKey alternateKey = Keys.hmacShaKeyFor(RAW_SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("customer@example.com")
                .issuer("different-issuer")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(alternateKey)
                .compact();

        assertThat(jwtUtil.validateToken(token)).isFalse();
        assertThatThrownBy(() -> jwtUtil.extractUsername(token))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        SecretKey key = Keys.hmacShaKeyFor(RAW_SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("customer@example.com")
                .issuer(ISSUER)
                .issuedAt(new Date(System.currentTimeMillis() - 120_000L))
                .expiration(new Date(System.currentTimeMillis() - 60_000L))
                .signWith(key)
                .compact();

        assertThat(jwtUtil.validateToken(token)).isFalse();
        assertThat(jwtUtil.isTokenValid(token, User.withUsername("customer@example.com")
                .password("encoded").roles("CUSTOMER").build())).isFalse();
    }

    @Test
    void blankSecretIsRejected() {
        assertThatThrownBy(() -> new JwtUtil("  ", 900_000L, ISSUER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT secret must not be empty.");
    }

    @Test
    void nonPositiveExpirationIsRejected() {
        assertThatThrownBy(() -> new JwtUtil(SECRET, 0L, ISSUER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT expiration must be greater than zero.");
    }

    @Test
    void blankIssuerIsRejected() {
        assertThatThrownBy(() -> new JwtUtil(SECRET, 900_000L, " "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT issuer must not be empty.");
    }
}
