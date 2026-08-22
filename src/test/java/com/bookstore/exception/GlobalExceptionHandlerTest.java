package com.bookstore.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void badRequestMapsToHttp400AndStandardErrorContract() {
        MockHttpServletRequest request = request("/api/books");
        request.addHeader("X-Request-Id", "checkout-123");

        ResponseEntity<?> response = handler.handleBadRequest(new BadRequestException("Invalid request"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        ApiErrorResponseTestSupport.assertContract(response, 400, "Invalid request", "/api/books", "checkout-123");
    }

    @Test
    void duplicateResourceMapsToHttp409() {
        ResponseEntity<?> response = handler.handleDuplicate(
                new DuplicateResourceException("Already exists"), request("/api/books"));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void resourceNotFoundMapsToHttp404() {
        ResponseEntity<?> response = handler.handleNotFound(
                new ResourceNotFoundException("Book", 10L), request("/api/books/10"));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void requestIdIsPropagatedToErrorResponseAndHeader() {
        MockHttpServletRequest request = request("/api/orders");
        request.addHeader("X-Request-Id", "order-456");

        ResponseEntity<?> response = handler.handleNotFound(
                new ResourceNotFoundException("Order", 7L), request);

        assertThat(response.getHeaders().getFirst("X-Request-Id")).isEqualTo("order-456");
        ApiErrorResponseTestSupport.assertContract(response, 404, "Order with id '7' was not found", "/api/orders", "order-456");
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }
}
