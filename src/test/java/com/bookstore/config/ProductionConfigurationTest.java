package com.bookstore.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionConfigurationTest {

    @Test
    void productionConfigurationRequiresExternalSecretsAndSecureSessions() throws IOException {
        String properties = readResource("application-prod.properties");

        assertThat(properties)
                .contains("app.jwt.secret=${JWT_SECRET}")
                .contains("spring.datasource.username=${DB_USERNAME}")
                .contains("spring.datasource.password=${DB_PASSWORD}")
                .contains("razorpay.key-id=${RAZORPAY_KEY_ID}")
                .contains("razorpay.key-secret=${RAZORPAY_KEY_SECRET}")
                .contains("server.servlet.session.cookie.http-only=true")
                .contains("server.servlet.session.cookie.same-site=lax")
                .contains("server.servlet.session.cookie.secure=true");
    }

    @Test
    void productionConfigurationKeepsActuatorExposureMinimal() throws IOException {
        String properties = readResource("application-prod.properties");

        assertThat(properties)
                .contains("management.endpoints.web.exposure.include=health,info")
                .doesNotContain("management.endpoints.web.exposure.include=*");
    }

    private String readResource(String resource) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertThat(input).as("resource %s", resource).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
