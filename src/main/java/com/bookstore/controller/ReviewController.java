package com.bookstore.controller;

import com.bookstore.dto.request.ReviewRequest;
import com.bookstore.dto.response.ApiResponse;
import com.bookstore.dto.response.ReviewResponse;
import com.bookstore.dto.response.ReviewSummaryResponse;
import com.bookstore.security.CustomUserDetails;
import com.bookstore.service.ReviewService;
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
public class ReviewController {
    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getReviews(@PathVariable Long bookId) {
        return ResponseEntity.ok(ApiResponse.success("Reviews retrieved successfully.", reviewService.getReviews(bookId)));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> summary(@PathVariable Long bookId) {
        return ResponseEntity.ok(ApiResponse.success("Review summary retrieved successfully.",
                ReviewSummaryResponse.builder()
                        .averageRating(reviewService.getAverageRating(bookId))
                        .reviewCount(reviewService.getReviewCount(bookId))
                        .build()));
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ReviewResponse>> create(Authentication auth,
                                                               @PathVariable Long bookId,
                                                               @Valid @RequestBody ReviewRequest request) {
        Long userId = ((CustomUserDetails) auth.getPrincipal()).getUser().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review created successfully.", reviewService.create(userId, bookId, request)));
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> delete(Authentication auth,
                                                     @PathVariable Long bookId,
                                                     @PathVariable Long reviewId) {
        Long userId = ((CustomUserDetails) auth.getPrincipal()).getUser().getId();
        reviewService.delete(userId, reviewId);
        return ResponseEntity.ok(ApiResponse.success("Review deleted successfully."));
    }
}
