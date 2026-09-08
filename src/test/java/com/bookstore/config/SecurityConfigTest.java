package com.bookstore.config;

import com.bookstore.security.AuthRateLimitingFilter;
import com.bookstore.security.CustomUserDetailsService;
import com.bookstore.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.util.ReflectionTestUtils.setField;

class SecurityConfigTest {

    private SecurityConfig newConfig() {
        return new SecurityConfig(
                (CustomUserDetailsService) null,
                (JwtAuthenticationFilter) null,
                (AuthRateLimitingFilter) null);
    }

    @Test
    void passwordEncoderUsesConfiguredBcryptStrength() {
        SecurityConfig config = newConfig();
        setField(config, "bcryptStrength", 10);
        BCryptPasswordEncoder encoder = (BCryptPasswordEncoder) config.passwordEncoder();
        String rawPassword = "bookstore-test-password";
        String encodedPassword = encoder.encode(rawPassword);
        assertThat(encodedPassword).startsWith("$2a$10$");
        assertThat(encoder.matches(rawPassword, encodedPassword)).isTrue();
    }

    @Test
    void passwordEncoderRejectsStrengthBelowMinimum() {
        SecurityConfig config = newConfig();
        setField(config, "bcryptStrength", 3);
        assertThatThrownBy(config::passwordEncoder).isInstanceOf(IllegalStateException.class)
                .hasMessage("security.password.bcrypt-strength must be between 4 and 31.");
    }

    @Test
    void passwordEncoderRejectsStrengthAboveMaximum() {
        SecurityConfig config = newConfig();
        setField(config, "bcryptStrength", 32);
        assertThatThrownBy(config::passwordEncoder).isInstanceOf(IllegalStateException.class)
                .hasMessage("security.password.bcrypt-strength must be between 4 and 31.");
    }

    @Test
    void passwordEncoderAcceptsMinimumAndMaximumStrengths() {
        SecurityConfig minimum = newConfig();
        setField(minimum, "bcryptStrength", 4);
        assertThat(((BCryptPasswordEncoder) minimum.passwordEncoder()).encode("password"))
                .startsWith("$2a$04$");

        SecurityConfig maximum = newConfig();
        setField(maximum, "bcryptStrength", 31);
        assertThat(maximum.passwordEncoder()).isInstanceOf(BCryptPasswordEncoder.class);
    }
}
