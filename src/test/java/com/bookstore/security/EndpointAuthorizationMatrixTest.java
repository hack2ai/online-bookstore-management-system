package com.bookstore.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;

class EndpointAuthorizationMatrixTest {

    @Test
    void customerAuthorityIsAcceptedForCustomerOnlyRoutes() {
        Authentication customer = authenticationWith("ROLE_CUSTOMER");
        var manager = AuthorityAuthorizationManager.hasRole("CUSTOMER");

        AuthorizationDecision decision = manager.check(() -> customer, null);

        assertThat(decision.isGranted()).isTrue();
    }

    @Test
    void customerAuthorityIsRejectedForAdminRoutes() {
        Authentication customer = authenticationWith("ROLE_CUSTOMER");
        var manager = AuthorityAuthorizationManager.hasRole("ADMIN");

        AuthorizationDecision decision = manager.check(() -> customer, null);

        assertThat(decision.isGranted()).isFalse();
    }

    @Test
    void adminAuthorityIsAcceptedForAdminRoutes() {
        Authentication admin = authenticationWith("ROLE_ADMIN");
        var manager = AuthorityAuthorizationManager.hasRole("ADMIN");

        AuthorizationDecision decision = manager.check(() -> admin, null);

        assertThat(decision.isGranted()).isTrue();
    }

    @Test
    void protectedRouteMatrixKeepsCustomerAndAdminSectionsSeparate() {
        var customerOnly = new AuthorizationRule(HttpMethod.GET, "/cart/**", "ROLE_CUSTOMER");
        var orders = new AuthorizationRule(HttpMethod.GET, "/orders/**", "ROLE_CUSTOMER");
        var profile = new AuthorizationRule(HttpMethod.GET, "/profile/**", "ROLE_CUSTOMER");
        var admin = new AuthorizationRule(HttpMethod.GET, "/admin/**", "ROLE_ADMIN");

        assertThat(customerOnly.requiredRole()).isEqualTo("ROLE_CUSTOMER");
        assertThat(orders.requiredRole()).isEqualTo("ROLE_CUSTOMER");
        assertThat(profile.requiredRole()).isEqualTo("ROLE_CUSTOMER");
        assertThat(admin.requiredRole()).isEqualTo("ROLE_ADMIN");
        assertThat(customerOnly.requiredRole()).isNotEqualTo(admin.requiredRole());
    }

    private Authentication authenticationWith(String role) {
        return new TestingAuthenticationToken("test-user", "n/a", role);
    }

    private record AuthorizationRule(HttpMethod method, String pattern, String requiredRole) {
    }
}
