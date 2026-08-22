package com.bookstore.service;

import com.bookstore.entity.Book;
import com.bookstore.entity.Order;
import com.bookstore.entity.OrderItem;
import com.bookstore.entity.OrderStatus;
import com.bookstore.entity.Role;
import com.bookstore.entity.User;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CartRepository;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCancellationTest {

    @Mock OrderRepository orderRepository;
    @Mock CartRepository cartRepository;
    @Mock BookRepository bookRepository;
    @Mock UserRepository userRepository;
    @Mock CouponService couponService;
    @Mock AuditService auditService;

    private OrderServiceImpl service;
    private User user;
    private Book book;
    private Order order;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(orderRepository, cartRepository, bookRepository, userRepository, couponService, auditService);
        user = User.builder().id(1L).name("Test").email("test@example.com").password("hash").role(Role.CUSTOMER).build();
        book = Book.builder().id(10L).title("Clean Code").price(new BigDecimal("500.00")).stock(3).build();
        order = Order.builder().id(100L).user(user).status(OrderStatus.PENDING)
                .shippingAddress("Bengaluru").subtotalAmount(new BigDecimal("1000.00"))
                .discountAmount(new BigDecimal("200.00")).couponCode("SAVE20")
                .totalAmount(new BigDecimal("800.00")).build();
        order.addItem(OrderItem.builder().book(book).quantity(2).price(new BigDecimal("500.00")).build());
    }

    @Test
    void cancellingPendingOrderRestoresStockAndReleasesCoupon() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(bookRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(book));

        service.cancelOrder(1L, 100L);

        assertThat(book.getStock()).isEqualTo(5);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(couponService).releaseReservation(1L, "SAVE20");
    }

    @Test
    void cancellingConfirmedOrderRestoresStockButKeepsCouponConsumed() {
        order.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(bookRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(book));

        service.cancelOrder(1L, 100L);

        assertThat(book.getStock()).isEqualTo(5);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(couponService, never()).releaseReservation(anyLong(), anyString());
    }
}
