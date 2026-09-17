package com.bookstore.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RequestCorrelationFilterTest {

    private RequestCorrelationFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new RequestCorrelationFilter();
        filterChain = mock(FilterChain.class);
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void generatesRequestIdAndAddsItToResponseAndMdcDuringRequest() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/books");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doAnswer(invocation -> {
            assertThat(MDC.get(RequestCorrelationFilter.MDC_KEY))
                    .isEqualTo(response.getHeader(RequestCorrelationFilter.HEADER_NAME));
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(request, response, filterChain);

        String requestId = response.getHeader(RequestCorrelationFilter.HEADER_NAME);
        assertThat(requestId).isNotBlank().hasSize(36);
        assertThat(MDC.get(RequestCorrelationFilter.MDC_KEY)).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void preservesSafeIncomingRequestId() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/orders");
        request.addHeader(RequestCorrelationFilter.HEADER_NAME, "order-2026:abc_123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(RequestCorrelationFilter.HEADER_NAME))
                .isEqualTo("order-2026:abc_123");
        assertThat(MDC.get(RequestCorrelationFilter.MDC_KEY)).isNull();
    }

    @Test
    void replacesUnsafeIncomingRequestId() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/books");
        request.addHeader(RequestCorrelationFilter.HEADER_NAME, "bad id with whitespace");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        String requestId = response.getHeader(RequestCorrelationFilter.HEADER_NAME);
        assertThat(requestId).isNotEqualTo("bad id with whitespace");
        assertThat(requestId).matches("[A-Za-z0-9._:-]{1,64}");
        assertThat(MDC.get(RequestCorrelationFilter.MDC_KEY)).isNull();
    }

    @Test
    void clearsMdcWhenTheRequestChainThrows() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/books");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RuntimeException failure = new RuntimeException("boom");
        doAnswer(invocation -> {
            assertThat(MDC.get(RequestCorrelationFilter.MDC_KEY)).isNotNull();
            throw failure;
        }).when(filterChain).doFilter(any(), any());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
                .isSameAs(failure);
        assertThat(MDC.get(RequestCorrelationFilter.MDC_KEY)).isNull();
    }

    private MockHttpServletRequest request(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setRequestURI(uri);
        return request;
    }
}
