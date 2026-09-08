package com.bookstore.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.util.ReflectionTestUtils.setField;

class SecurityConfigTest {

    @Test
    void passwordEncoderUsesConfiguredBcryptStrength() {
        SecurityConfig config = new SecurityConfig(null, null);
        setField(config, "bcryptStrength", 10);

        BCryptPasswordEncoder encoder = (BCryptPasswordEncoder) config.passwordEncoder();
        String rawPassword = "bookstore-test-password";
        String encodedPassword = encoder.encode(rawPassword);

        assertThat(encoder.getStrength()).isEqualTo(10);
        assertThat(encoder.matches(rawPassword, encodedPassword)).isTrue();
    }

    @Test
    void passwordEncoderRejectsStrengthBelowMinimum() {
        SecurityConfig config = new SecurityConfig(null, null);
        setField(config, "bcryptStrength", 3);

        assertThatThrownBy(config::passwordEncoder)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("security.password.bcrypt-strength must be between 4 and 31.");
    }

    @Test
    void passwordEncoderRejectsStrengthAboveMaximum() {
        SecurityConfig config = new SecurityConfig(null, null);
        setField(config, "bcryptStrength", 32);

        assertThatThrownBy(config::passwordEncoder)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("security.password.bcrypt-strength must be between 4 and 31.");
    }

    @Test
    void passwordEncoderAcceptsMinimumAndMaximumStrengths() {
        SecurityConfig minimum = new SecurityConfig(null, null);
        setField(minimum, "bcryptStrength", 4);
        assertThat(((BCryptPasswordEncoder) minimum.passwordEncoder()).getStrength()).isEqualTo(4);

        SecurityConfig maximum = new SecurityConfig(null, null);
        setField(maximum, "bcryptStrength", 31);
        assertThat(((BCryptPasswordEncoder) maximum.passwordEncoder()).getStrength()).isEqualTo(31);
    }
}
