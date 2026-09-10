package com.bookstore.controller;

import com.bookstore.dto.request.ReviewRequest;
import com.bookstore.dto.response.ApiResponse;
import com.bookstore.dto.response.ReviewResponse;
import com.bookstore.dto.response.ReviewSummaryResponse;
import com.bookstore.security.CustomUserDetails;
import com.bookstore.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books/{bookId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Book reviews and ratings")
public class ReviewController {
    private final ReviewService reviewService;

    @GetMapping
    @Operation(summary = "Get book reviews")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getReviews(
            @Parameter(name = "bookId", in = ParameterIn.PATH, description = "Book ID", required = true)
            @PathVariable Long bookId) {
        return ResponseEntity.ok(ApiResponse.success("Reviews retrieved successfully.", reviewService.getReviews(bookId)));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get review summary")
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> summary(
            @Parameter(name = "bookId", in = ParameterIn.PATH, description = "Book ID", required = true)
            @PathVariable Long bookId) {
        return ResponseEntity.ok(ApiResponse.success("Review summary retrieved successfully.",
                ReviewSummaryResponse.builder()
                        .averageRating(reviewService.getAverageRating(bookId))
                        .reviewCount(reviewService.getReviewCount(bookId))
                        .build()));
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a book review", description = "Creates a review for a book using the authenticated customer account.")
    public ResponseEntity<ApiResponse<ReviewResponse>> create(
            Authentication auth,
            @Parameter(name = "bookId", in = ParameterIn.PATH, description = "Book ID", required = true)
            @PathVariable Long bookId,
            @Valid @RequestBody ReviewRequest request) {
        Long userId = ((CustomUserDetails) auth.getPrincipal()).getUser().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review created successfully.", reviewService.create(userId, bookId, request)));
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete my review", description = "Deletes a review owned by the authenticated customer.")
    public ResponseEntity<ApiResponse<Void>> delete(
            Authentication auth,
            @Parameter(name = "bookId", in = ParameterIn.PATH, description = "Book ID", required = true)
            @PathVariable Long bookId,
            @Parameter(name = "reviewId", in = ParameterIn.PATH, description = "Review ID", required = true)
            @PathVariable Long reviewId) {
        Long userId = ((CustomUserDetails) auth.getPrincipal()).getUser().getId();
        reviewService.delete(userId, reviewId);
        return ResponseEntity.ok(ApiResponse.success("Review deleted successfully."));
    }
}
