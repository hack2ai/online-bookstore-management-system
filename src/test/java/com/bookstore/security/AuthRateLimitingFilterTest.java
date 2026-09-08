package com.bookstore.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class AuthRateLimitingFilterTest {

    private AuthRateLimitingFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new AuthRateLimitingFilter(new ObjectMapper());
        filterChain = mock(FilterChain.class);
    }

    @Test
    void allowsTenAuthenticationRequestsAndRejectsTheEleventh() throws Exception {
        for (int attempt = 1; attempt <= 10; attempt++) {
            MockHttpServletResponse response = doFilter("/api/auth/login", "POST", "10.0.0.10");
            assertThat(response.getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse rejected = doFilter("/api/auth/login", "POST", "10.0.0.10");

        assertThat(rejected.getStatus()).isEqualTo(429);
        assertThat(rejected.getHeader("Retry-After")).isEqualTo("60");
        assertThat(rejected.getContentType()).isEqualTo("application/json");
        assertThat(rejected.getContentAsString()).contains("Too many authentication requests");
        verify(filterChain, times(10)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verifyNoMoreInteractions(filterChain);
    }

    @Test
    void keepsLimitsIndependentAcrossEndpointsAndClientAddresses() throws Exception {
        for (int attempt = 1; attempt <= 10; attempt++) {
            doFilter("/api/auth/login", "POST", "10.0.0.20");
        }

        MockHttpServletResponse refreshResponse = doFilter("/api/auth/refresh", "POST", "10.0.0.20");
        MockHttpServletResponse otherClientResponse = doFilter("/api/auth/login", "POST", "10.0.0.21");

        assertThat(refreshResponse.getStatus()).isEqualTo(200);
        assertThat(otherClientResponse.getStatus()).isEqualTo(200);
        verify(filterChain, times(12)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void bypassesNonAuthenticationRequests() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/books");
        request.setRemoteAddr("10.0.0.30");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(filterChain).doFilter(request, response);
    }

    private MockHttpServletResponse doFilter(String uri, String method, String remoteAddr)
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setRequestURI(uri);
        request.setRemoteAddr(remoteAddr);

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, filterChain);
        return response;
    }
}
