package com.bookstore.exception;

import com.bookstore.dto.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void badRequestMapsToHttp400() {
        ResponseEntity<?> response = handler.handleBadRequest(new BadRequestException("Invalid request"));
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void badRequestUsesFallbackMessageWhenExceptionMessageIsBlank() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleBadRequest(new IllegalArgumentException("  "));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("The request could not be processed.");
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

    @Test
    void validationErrorsMapToHttp400AndExposeFieldMessages() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        var bindingResult = mock(org.springframework.validation.BindingResult.class);
        FieldError titleError = new FieldError("bookRequest", "title", "Title is required");
        FieldError priceError = new FieldError("bookRequest", "price", "Price must be positive");

        org.mockito.Mockito.when(exception.getBindingResult()).thenReturn(bindingResult);
        org.mockito.Mockito.when(bindingResult.getFieldErrors()).thenReturn(List.of(titleError, priceError));

        ResponseEntity<ApiResponse<java.util.Map<String, String>>> response = handler.handleValidationErrors(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(response.getBody().getData())
                .containsEntry("title", "Title is required")
                .containsEntry("price", "Price must be positive");
    }

    @Test
    void validationErrorsKeepFirstMessageForDuplicateField() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        var bindingResult = mock(org.springframework.validation.BindingResult.class);
        FieldError first = new FieldError("bookRequest", "title", "Title is required");
        FieldError second = new FieldError("bookRequest", "title", "Title is too long");

        org.mockito.Mockito.when(exception.getBindingResult()).thenReturn(bindingResult);
        org.mockito.Mockito.when(bindingResult.getFieldErrors()).thenReturn(List.of(first, second));

        ResponseEntity<ApiResponse<java.util.Map<String, String>>> response = handler.handleValidationErrors(exception);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).containsEntry("title", "Title is required");
    }

    @Test
    void constraintViolationsMapToHttp400AndExposePropertyPaths() {
        jakarta.validation.ConstraintViolationException exception = mock(jakarta.validation.ConstraintViolationException.class);
        jakarta.validation.ConstraintViolation<?> violation = mock(jakarta.validation.ConstraintViolation.class);
        jakarta.validation.Path path = mock(jakarta.validation.Path.class);

        org.mockito.Mockito.when(exception.getConstraintViolations()).thenReturn(java.util.Set.of(violation));
        org.mockito.Mockito.when(violation.getPropertyPath()).thenReturn(path);
        org.mockito.Mockito.when(path.toString()).thenReturn("search.size");
        org.mockito.Mockito.when(violation.getMessage()).thenReturn("must be less than or equal to 100");

        ResponseEntity<ApiResponse<java.util.Map<String, String>>> response = handler.handleConstraintViolations(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData())
                .containsEntry("search.size", "must be less than or equal to 100");
    }
}
