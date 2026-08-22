package com.bookstore.exception;

import com.bookstore.dto.response.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void badRequestMapsToHttp400AndStandardErrorContract() {
        MockHttpServletRequest request = request("/api/books");
        request.addHeader("X-Request-Id", "checkout-123");

        ResponseEntity<ApiErrorResponse> response = handler.handleBadRequest(
                new BadRequestException("Invalid request"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid request");
        assertThat(response.getBody().getPath()).isEqualTo("/api/books");
        assertThat(response.getBody().getRequestId()).isEqualTo("checkout-123");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void duplicateResourceMapsToHttp409() {
        ResponseEntity<ApiErrorResponse> response = handler.handleDuplicate(
                new DuplicateResourceException("Already exists"), request("/api/books"));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Conflict");
    }

    @Test
    void resourceNotFoundMapsToHttp404() {
        ResponseEntity<ApiErrorResponse> response = handler.handleNotFound(
                new ResourceNotFoundException("Book", 10L), request("/api/books/10"));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getPath()).isEqualTo("/api/books/10");
    }

    @Test
    void requestIdIsPropagatedToErrorResponseAndHeader() {
        MockHttpServletRequest request = request("/api/orders");
        request.addHeader("X-Request-Id", "order-456");

        ResponseEntity<ApiErrorResponse> response = handler.handleNotFound(
                new ResourceNotFoundException("Order", 7L), request);

        assertThat(response.getHeaders().getFirst("X-Request-Id")).isEqualTo("order-456");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRequestId()).isEqualTo("order-456");
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }
}
