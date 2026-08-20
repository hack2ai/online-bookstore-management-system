package com.bookstore.service;

import com.bookstore.dto.request.ReviewRequest;
import com.bookstore.dto.response.ReviewResponse;
import java.util.List;

public interface ReviewService {
    List<ReviewResponse> getReviews(Long bookId);
    double getAverageRating(Long bookId);
    long getReviewCount(Long bookId);
    ReviewResponse create(Long userId, Long bookId, ReviewRequest request);
    void delete(Long userId, Long reviewId);
}
