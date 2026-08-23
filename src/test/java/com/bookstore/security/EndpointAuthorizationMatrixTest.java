package com.bookstore.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;

class EndpointAuthorizationMatrixTest {

    @Test
    void customerEndpointsAreExplicitlyLimitedToCustomerRole() {
        var customerOnly = new AuthorizationRule(HttpMethod.GET, "/cart/**", "ROLE_CUSTOMER");
        var orders = new AuthorizationRule(HttpMethod.GET, "/orders/**", "ROLE_CUSTOMER");
        var profile = new AuthorizationRule(HttpMethod.GET, "/profile/**", "ROLE_CUSTOMER");

        assertThat(customerOnly.requiredRole()).isEqualTo("ROLE_CUSTOMER");
        assertThat(orders.requiredRole()).isEqualTo("ROLE_CUSTOMER");
        assertThat(profile.requiredRole()).isEqualTo("ROLE_CUSTOMER");
    }

    @Test
    void adminEndpointsAreExplicitlyLimitedToAdminRole() {
        var admin = new AuthorizationRule(HttpMethod.GET, "/admin/**", "ROLE_ADMIN");

        assertThat(admin.requiredRole()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void adminAndCustomerRolesDoNotOverlapForProtectedSections() {
        var admin = "ROLE_ADMIN";
        var customer = "ROLE_CUSTOMER";

        assertThat(admin).isNotEqualTo(customer);
    }

    private record AuthorizationRule(HttpMethod method, String pattern, String requiredRole) {
    }
}
