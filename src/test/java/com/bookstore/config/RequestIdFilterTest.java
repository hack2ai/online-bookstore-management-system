package com.bookstore.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void echoesSafeCallerRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(RequestIdFilter.HEADER_NAME, "checkout-123");

        FilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);

        assertEquals("checkout-123", response.getHeader(RequestIdFilter.HEADER_NAME));
    }

    @Test
    void generatesRequestIdWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String requestId = response.getHeader(RequestIdFilter.HEADER_NAME);
        assertNotNull(requestId);
        assertTrue(requestId.matches("[0-9a-fA-F-]{36}"));
    }

    @Test
    void rejectsControlCharactersAndGeneratesSafeId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(RequestIdFilter.HEADER_NAME, "bad\r\nheader");

        filter.doFilter(request, response, new MockFilterChain());

        String requestId = response.getHeader(RequestIdFilter.HEADER_NAME);
        assertNotNull(requestId);
        assertNull(requestId.contains("\r") || requestId.contains("\n") ? requestId : null);
    }
}
