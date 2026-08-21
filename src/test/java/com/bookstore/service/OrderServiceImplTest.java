package com.bookstore.service;

import com.bookstore.dto.request.CheckoutRequest;
import com.bookstore.entity.Book;
import com.bookstore.entity.Cart;
import com.bookstore.entity.CartItem;
import com.bookstore.entity.User;
import com.bookstore.entity.Role;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CartRepository;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.dto.response.DiscountResponse;
import com.bookstore.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock CartRepository cartRepository;
    @Mock BookRepository bookRepository;
    @Mock OrderRepository orderRepository;
    @Mock UserRepository userRepository;
    @Mock CouponService couponService;

    private OrderServiceImpl service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(orderRepository, cartRepository, bookRepository, userRepository, couponService);
        user = User.builder().id(1L).name("Test User").email("test@example.com").password("hash").role(Role.CUSTOMER).build();
    }

    @Test
    void checkoutCreatesFrozenPriceSnapshotAndClearsCart() {
        Book book = Book.builder().id(10L).title("Clean Code").price(new BigDecimal("500.00")).stock(5).build();
        CartItem item = CartItem.builder().book(book).quantity(2).build();
        Cart cart = Cart.builder().id(20L).user(user).items(new ArrayList<>()).build();
        cart.getItems().add(item);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(book));
        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.placeOrder(1L, CheckoutRequest.builder().shippingAddress("Bengaluru").build());

        assertThat(response.getSubtotalAmount()).isEqualByComparingTo("1000.00");
        assertThat(response.getDiscountAmount()).isEqualByComparingTo("0.00");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("1000.00");
        assertThat(book.getStock()).isEqualTo(3);
        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository).save(cart);

        ArgumentCaptor<com.bookstore.entity.Order> captor = ArgumentCaptor.forClass(com.bookstore.entity.Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderItems()).hasSize(1);
        assertThat(captor.getValue().getOrderItems().getFirst().getPrice()).isEqualByComparingTo("500.00");
    }

    @Test
    void checkoutAppliesCouponToFinalTotal() {
        Book book = Book.builder().id(10L).title("Clean Code").price(new BigDecimal("500.00")).stock(5).build();
        CartItem item = CartItem.builder().book(book).quantity(2).build();
        Cart cart = Cart.builder().id(20L).user(user).items(new ArrayList<>()).build();
        cart.getItems().add(item);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(book));
        when(couponService.calculateAndReserve(eq(1L), eq("SAVE20"), eq(new BigDecimal("1000.00")), eq(user)))
                .thenReturn(DiscountResponse.builder().code("SAVE20").discount(new BigDecimal("200.00")).build());
        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.placeOrder(1L, CheckoutRequest.builder().shippingAddress("Bengaluru").couponCode("SAVE20").build());

        assertThat(response.getSubtotalAmount()).isEqualByComparingTo("1000.00");
        assertThat(response.getDiscountAmount()).isEqualByComparingTo("200.00");
        assertThat(response.getCouponCode()).isEqualTo("SAVE20");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("800.00");
    }

    @Test
    void checkoutRejectsInsufficientStock() {
        Book book = Book.builder().id(10L).title("Clean Code").price(new BigDecimal("500.00")).stock(1).build();
        CartItem item = CartItem.builder().book(book).quantity(2).build();
        Cart cart = Cart.builder().id(20L).user(user).items(new ArrayList<>()).build();
        cart.getItems().add(item);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> service.placeOrder(1L, CheckoutRequest.builder().shippingAddress("Bengaluru").build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkoutRejectsEmptyCart() {
        Cart cart = Cart.builder().id(20L).user(user).items(new ArrayList<>()).build();
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> service.placeOrder(1L, CheckoutRequest.builder().shippingAddress("Bengaluru").build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cart is empty");
    }
}
