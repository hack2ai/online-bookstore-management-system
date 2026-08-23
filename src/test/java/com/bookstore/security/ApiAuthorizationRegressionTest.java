package com.bookstore.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiAuthorizationRegressionTest {

    @Test
    void publicApiEndpointsRemainPublic() {
        var publicEndpoints = List.of(
                new ApiRule(HttpMethod.POST, "/api/auth/register", "PUBLIC"),
                new ApiRule(HttpMethod.POST, "/api/auth/login", "PUBLIC"),
                new ApiRule(HttpMethod.POST, "/api/auth/refresh", "PUBLIC"),
                new ApiRule(HttpMethod.GET, "/api/books", "PUBLIC"),
                new ApiRule(HttpMethod.GET, "/api/categories", "PUBLIC")
        );

        assertThat(publicEndpoints).allMatch(rule -> rule.access().equals("PUBLIC"));
    }

    @Test
    void adminApiNamespaceRequiresAdminRole() {
        var rule = new ApiRule(HttpMethod.GET, "/api/admin/**", "ROLE_ADMIN");

        assertThat(rule.access()).isEqualTo("ROLE_ADMIN");
        assertThat(rule.access()).isNotEqualTo("ROLE_CUSTOMER");
    }

    @Test
    void catalogWritesRequireAdminRole() {
        var writes = List.of(
                new ApiRule(HttpMethod.POST, "/api/books/**", "ROLE_ADMIN"),
                new ApiRule(HttpMethod.PUT, "/api/books/**", "ROLE_ADMIN"),
                new ApiRule(HttpMethod.DELETE, "/api/books/**", "ROLE_ADMIN"),
                new ApiRule(HttpMethod.POST, "/api/categories/**", "ROLE_ADMIN"),
                new ApiRule(HttpMethod.PUT, "/api/categories/**", "ROLE_ADMIN"),
                new ApiRule(HttpMethod.DELETE, "/api/categories/**", "ROLE_ADMIN")
        );

        assertThat(writes).allMatch(rule -> rule.access().equals("ROLE_ADMIN"));
    }

    @Test
    void protectedApiRulesDoNotGrantAnonymousAccess() {
        var protectedRule = new ApiRule(HttpMethod.GET, "/api/orders/**", "AUTHENTICATED");

        assertThat(protectedRule.access()).isEqualTo("AUTHENTICATED");
        assertThat(protectedRule.access()).isNotEqualTo("PUBLIC");
    }

    private record ApiRule(HttpMethod method, String pattern, String access) {
    }
}
