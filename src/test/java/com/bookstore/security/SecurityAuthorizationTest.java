package com.bookstore.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityAuthorizationTest {

    @Test
    void customerAndAdminRolesRemainDistinct() {
        var customer = new TestingAuthenticationToken(
                "customer", null, new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        var admin = new TestingAuthenticationToken(
                "admin", null, new SimpleGrantedAuthority("ROLE_ADMIN"));

        assertThat(customer.getAuthorities()).extracting(a -> a.getAuthority())
                .containsExactly("ROLE_CUSTOMER");
        assertThat(admin.getAuthorities()).extracting(a -> a.getAuthority())
                .containsExactly("ROLE_ADMIN");
        assertThat(customer.getAuthorities()).doesNotContainAnyElementsOf(admin.getAuthorities());
    }

    @Test
    void customerMustNotCarryAdminAuthority() {
        var customer = new TestingAuthenticationToken(
                "customer", null, new SimpleGrantedAuthority("ROLE_CUSTOMER"));

        assertThat(customer.getAuthorities()).extracting(a -> a.getAuthority())
                .doesNotContain("ROLE_ADMIN");
    }
}
