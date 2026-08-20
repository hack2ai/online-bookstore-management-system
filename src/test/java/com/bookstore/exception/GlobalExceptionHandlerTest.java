package com.bookstore.exception;

import com.bookstore.controller.CartController;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void badRequestMapsToHttp400() {
        ResponseEntity<?> response = handler.handleBadRequest(new BadRequestException("Invalid request"));
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void duplicateResourceMapsToHttp409() {
        ResponseEntity<?> response = handler.handleDuplicate(new DuplicateResourceException("Already exists"));
        assertThat(response.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void resourceNotFoundMapsToHttp404() {
        ResponseEntity<?> response = handler.handleNotFound(new ResourceNotFoundException("Book", 10L));
        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }
}
