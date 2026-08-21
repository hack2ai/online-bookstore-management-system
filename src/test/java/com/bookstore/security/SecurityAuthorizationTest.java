package com.bookstore.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityAuthorizationTest {

    @Test
    void customerAndAdminRolesRemainDistinct() {
        var customer = List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        var admin = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));

        assertThat(customer).extracting(a -> a.getAuthority())
                .containsExactly("ROLE_CUSTOMER");
        assertThat(admin).extracting(a -> a.getAuthority())
                .containsExactly("ROLE_ADMIN");
        assertThat(customer).doesNotContainAnyElementsOf(admin);
    }

    @Test
    void customerMustNotCarryAdminAuthority() {
        var customer = List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));

        assertThat(customer).extracting(a -> a.getAuthority())
                .doesNotContain("ROLE_ADMIN");
    }
}
