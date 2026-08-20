package com.bookstore.service.impl;

import com.bookstore.dto.request.ReviewRequest;
import com.bookstore.dto.response.ReviewResponse;
import com.bookstore.entity.Book;
import com.bookstore.entity.Review;
import com.bookstore.entity.User;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.ReviewRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;

    @Override
    public List<ReviewResponse> getReviews(Long bookId) {
        return reviewRepository.findByBookIdOrderByCreatedAtDesc(bookId).stream().map(this::toResponse).toList();
    }

    @Override
    public double getAverageRating(Long bookId) {
        return reviewRepository.averageRating(bookId);
    }

    @Override
    public long getReviewCount(Long bookId) {
        return reviewRepository.countByBookId(bookId);
    }

    @Override
    @Transactional
    public ReviewResponse create(Long userId, Long bookId, ReviewRequest request) {
        if (reviewRepository.existsByUserIdAndBookId(userId, bookId)) {
            throw new IllegalStateException("You have already reviewed this book.");
        }
        if (!orderRepository.hasSuccessfulPurchaseOfBook(userId, bookId)) {
            throw new IllegalStateException("Only customers who purchased this book can review it.");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new ResourceNotFoundException("Book", bookId));
        return toResponse(reviewRepository.save(Review.builder().user(user).book(book).rating(request.getRating()).comment(request.getComment()).build()));
    }

    @Override
    @Transactional
    public void delete(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId).orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));
        if (!review.getUser().getId().equals(userId)) throw new IllegalStateException("You can only delete your own review.");
        reviewRepository.delete(review);
    }

    private ReviewResponse toResponse(Review r) {
        return ReviewResponse.builder().id(r.getId()).bookId(r.getBook().getId()).customerName(r.getUser().getName())
                .rating(r.getRating()).comment(r.getComment()).createdAt(r.getCreatedAt()).build();
    }
}
