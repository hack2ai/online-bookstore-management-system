package com.bookstore.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        validator.close();
    }

    @Test
    void acceptsValidRegistrationData() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Groot")
                .email("groot@example.com")
                .password("Secret123!")
                .phone("9876543210")
                .address("Hyderabad")
                .build();

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsBlankNameEmailAndPassword() {
        RegisterRequest request = RegisterRequest.builder()
                .name(" ")
                .email(" ")
                .password(" ")
                .build();

        Set<String> fields = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertThat(fields).contains("name", "email", "password");
    }

    @Test
    void rejectsMalformedEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Groot")
                .email("not-an-email")
                .password("Secret123!")
                .build();

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("email")
                        && violation.getMessage().equals("Invalid email format"));
    }

    @Test
    void rejectsPasswordOutsideConfiguredLengthRange() {
        RegisterRequest tooShort = RegisterRequest.builder()
                .name("Groot")
                .email("groot@example.com")
                .password("short")
                .build();

        RegisterRequest tooLong = RegisterRequest.builder()
                .name("Groot")
                .email("groot@example.com")
                .password("a".repeat(73))
                .build();

        assertThat(validator.validate(tooShort))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("password")
                        && violation.getMessage().equals("Password must be between 8 and 72 characters"));
        assertThat(validator.validate(tooLong))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("password")
                        && violation.getMessage().equals("Password must be between 8 and 72 characters"));
    }

    @Test
    void rejectsOversizedContactFields() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Groot")
                .email("groot@example.com")
                .password("Secret123!")
                .phone("1".repeat(21))
                .address("a".repeat(256))
                .build();

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("phone", "address");
    }
}
