package com.bookstore.controller;

import com.bookstore.dto.request.BookRequest;
import com.bookstore.dto.response.ApiResponse;
import com.bookstore.dto.response.BookResponse;
import com.bookstore.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Tag(name = "Books", description = "Public catalog and admin book management")
public class BookController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_KEYWORD_LENGTH = 100;

    private final BookService bookService;

    @GetMapping
    @Operation(summary = "Search and browse books")
    public ResponseEntity<ApiResponse<Page<BookResponse>>> search(
            @Parameter(description = "Title/author keyword")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Filter by category ID")
            @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Zero-based page number")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of books per page, 1-100")
            @RequestParam(defaultValue = "12") int size,
            @Parameter(description = "Allowed values: title, author, price, stock, createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "asc or desc")
            @RequestParam(defaultValue = "desc") String direction) {

        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (categoryId != null && categoryId <= 0) {
            throw new IllegalArgumentException("categoryId must be greater than zero");
        }

        String normalizedKeyword = normalizeKeyword(keyword);
        String safeSort = resolveSortProperty(sortBy);
        Sort.Direction sortDirection = resolveSortDirection(direction);

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, safeSort));
        return ResponseEntity.ok(ApiResponse.success(
                "Books retrieved successfully.",
                bookService.search(normalizedKeyword, categoryId, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a book by ID")
    public ResponseEntity<ApiResponse<BookResponse>> getById(@PathVariable Long id) {
        validateId(id, "Book ID");
        return ResponseEntity.ok(ApiResponse.success(
                "Book retrieved successfully.", bookService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a book (admin)")
    public ResponseEntity<ApiResponse<BookResponse>> create(@Valid @RequestBody BookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Book created successfully.", bookService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update a book (admin)")
    public ResponseEntity<ApiResponse<BookResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody BookRequest request) {
        validateId(id, "Book ID");
        return ResponseEntity.ok(ApiResponse.success(
                "Book updated successfully.", bookService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a book (admin)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        validateId(id, "Book ID");
        bookService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Book deleted successfully."));
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > MAX_KEYWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "keyword must be at most " + MAX_KEYWORD_LENGTH + " characters");
        }
        return normalized;
    }

    private String resolveSortProperty(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "createdAt";
        }
        return switch (sortBy.trim()) {
            case "title", "author", "price", "stock", "createdAt" -> sortBy.trim();
            default -> throw new IllegalArgumentException(
                    "Unsupported sortBy. Allowed values: title, author, price, stock, createdAt");
        };
    }

    private Sort.Direction resolveSortDirection(String direction) {
        if (direction == null || direction.isBlank()) {
            return Sort.Direction.DESC;
        }
        if ("asc".equalsIgnoreCase(direction.trim())) {
            return Sort.Direction.ASC;
        }
        if ("desc".equalsIgnoreCase(direction.trim())) {
            return Sort.Direction.DESC;
        }
        throw new IllegalArgumentException("direction must be either asc or desc");
    }

    private void validateId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
    }
}
