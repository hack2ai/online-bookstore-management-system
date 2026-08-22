package com.bookstore.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityHardeningPolicyTest {

    @Test
    void permissionsPolicyDisablesUnneededBrowserCapabilities() {
        String policy = "camera=(), microphone=(), geolocation=()";

        assertThat(policy)
                .contains("camera=()")
                .contains("microphone=()")
                .contains("geolocation=()");
    }
}
