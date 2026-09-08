package com.bookstore.service.impl;

import com.bookstore.entity.Order;
import com.bookstore.entity.User;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CartRepository;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.service.CouponService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CouponService couponService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void customerCannotReadAnotherCustomersOrder() {
        User owner = User.builder().id(10L).email("owner@example.com").build();
        Order order = Order.builder().id(77L).user(owner).build();
        when(orderRepository.findById(77L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getMyOrder(20L, 77L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found with id: 77");

        verify(orderRepository).findById(77L);
        verifyNoMoreInteractions(orderRepository, cartRepository, bookRepository, userRepository, couponService);
    }
}
