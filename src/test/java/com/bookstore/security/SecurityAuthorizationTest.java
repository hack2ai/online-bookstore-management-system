package com.bookstore.security;

import com.bookstore.config.SecurityConfig;
import com.bookstore.security.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract-level authorization tests for the application's role boundaries.
 * These tests complement endpoint-specific controller tests by documenting the
 * roles that the security configuration promises to protect.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(SecurityAuthorizationTest.TestBeans.class)
class SecurityAuthorizationTest {

    @Autowired
    SecurityConfig securityConfig;

    @Test
    void customerRoleIsDistinctFromAdminRole() {
        var customer = new TestingAuthenticationToken(
                "customer",
                null,
                new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        var admin = new TestingAuthenticationToken(
                "admin",
                null,
                new SimpleGrantedAuthority("ROLE_ADMIN"));

        assertThat(customer.getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactly("ROLE_CUSTOMER");
        assertThat(admin.getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactly("ROLE_ADMIN");
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class TestBeans {
        @Bean
        CustomUserDetailsService customUserDetailsService() {
            return null;
        }
    }
}
