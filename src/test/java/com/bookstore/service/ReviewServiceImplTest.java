package com.bookstore.service;

import com.bookstore.dto.request.ReviewRequest;
import com.bookstore.entity.Book;
import com.bookstore.entity.Role;
import com.bookstore.entity.User;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.ReviewRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {
    @Mock ReviewRepository reviewRepository;
    @Mock UserRepository userRepository;
    @Mock BookRepository bookRepository;
    @Mock OrderRepository orderRepository;

    private ReviewServiceImpl service;
    private User user;
    private Book book;

    @BeforeEach
    void setUp() {
        service = new ReviewServiceImpl(reviewRepository, userRepository, bookRepository, orderRepository);
        user = User.builder().id(1L).name("Customer").email("customer@example.com").password("hash").role(Role.CUSTOMER).build();
        book = Book.builder().id(10L).title("Clean Code").build();
    }

    @Test
    void duplicateReviewIsRejected() {
        when(reviewRepository.existsByUserIdAndBookId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(1L, 10L, request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already reviewed");

        verifyNoInteractions(orderRepository, userRepository, bookRepository);
    }

    @Test
    void customerWithoutSuccessfulPurchaseIsRejected() {
        when(reviewRepository.existsByUserIdAndBookId(1L, 10L)).thenReturn(false);
        when(orderRepository.hasSuccessfulPurchaseOfBook(1L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> service.create(1L, 10L, request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("purchased this book");
    }

    @Test
    void missingUserIsRejectedAfterPurchaseVerification() {
        when(reviewRepository.existsByUserIdAndBookId(1L, 10L)).thenReturn(false);
        when(orderRepository.hasSuccessfulPurchaseOfBook(1L, 10L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(1L, 10L, request()))
                .isInstanceOf(com.bookstore.exception.ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    private ReviewRequest request() {
        ReviewRequest request = new ReviewRequest();
        request.setRating(5);
        request.setComment("Excellent book.");
        return request;
    }
}
