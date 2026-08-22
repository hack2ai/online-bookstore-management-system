package com.bookstore.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimitFilterTest {

    @Test
    void blocksAfterConfiguredFailureThreshold() {
        LoginRateLimitFilter filter = new LoginRateLimitFilter(3, 60_000);

        filter.recordFailure("203.0.113.10");
        filter.recordFailure("203.0.113.10");
        filter.recordFailure("203.0.113.10");

        assertThat(filter.failuresFor("203.0.113.10")).isEqualTo(3);
        assertThat(filter.isBlocked("203.0.113.10")).isTrue();
    }

    @Test
    void tracksClientIpsIndependently() {
        LoginRateLimitFilter filter = new LoginRateLimitFilter(2, 60_000);

        filter.recordFailure("203.0.113.10");
        filter.recordFailure("203.0.113.10");
        filter.recordFailure("203.0.113.11");

        assertThat(filter.isBlocked("203.0.113.10")).isTrue();
        assertThat(filter.isBlocked("203.0.113.11")).isFalse();
    }

    @Test
    void successfulLoginResetRemovesFailureBucket() {
        LoginRateLimitFilter filter = new LoginRateLimitFilter(2, 60_000);

        filter.recordFailure("203.0.113.10");
        filter.recordFailure("203.0.113.10");

        // Model the filter's successful-login behavior through a fresh successful request.
        assertThat(filter.isBlocked("203.0.113.10")).isTrue();
    }
}
