package com.bookstore.service;

import com.bookstore.dto.request.CheckoutRequest;
import com.bookstore.dto.request.OrderStatusRequest;
import com.bookstore.dto.response.DiscountResponse;
import com.bookstore.entity.Book;
import com.bookstore.entity.Cart;
import com.bookstore.entity.CartItem;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
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

    @Test
    void checkoutRejectsInvalidUserIdBeforeRepositoryAccess() {
        assertThatThrownBy(() -> service.placeOrder(0L, CheckoutRequest.builder().shippingAddress("Bengaluru").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID must be greater than zero");
        verifyNoInteractions(cartRepository, userRepository, bookRepository, orderRepository, couponService);
    }

    @Test
    void checkoutRejectsBlankShippingAddress() {
        assertThatThrownBy(() -> service.placeOrder(1L, CheckoutRequest.builder().shippingAddress("   ").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Shipping address is required");
        verifyNoInteractions(cartRepository, userRepository, bookRepository, orderRepository, couponService);
    }

    @Test
    void cancelPendingOrderRestoresStockAndReleasesCoupon() {
        Book book = Book.builder().id(10L).title("Clean Code").price(new BigDecimal("500.00")).stock(3).build();
        Order order = Order.builder().id(100L).user(user).status(OrderStatus.PENDING)
                .couponCode("SAVE20").discountAmount(new BigDecimal("100.00")).build();
        OrderItem item = OrderItem.builder().order(order).book(book).quantity(2).price(new BigDecimal("500.00")).build();
        order.setOrderItems(new ArrayList<>(List.of(item)));

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(bookRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(book));

        service.cancelOrder(1L, 100L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(book.getStock()).isEqualTo(5);
        verify(couponService).releaseReservation(1L, "SAVE20");
    }

    @Test
    void cannotCancelShippedOrder() {
        Order order = Order.builder().id(100L).user(user).status(OrderStatus.SHIPPED).build();
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancelOrder(1L, 100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only pending or confirmed orders can be cancelled");
        verifyNoInteractions(bookRepository, couponService);
    }

    @Test
    void validatesOrderStatusTransitions() {
        Order order = Order.builder().id(100L).user(user).status(OrderStatus.PENDING).build();
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateStatus(100L, OrderStatus.CONFIRMED);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository).save(order);
    }

    @Test
    void rejectsInvalidOrderStatusTransition() {
        Order order = Order.builder().id(100L).user(user).status(OrderStatus.PENDING).build();
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.updateStatus(100L, OrderStatus.DELIVERED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid order status transition");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void getMyOrderRejectsOrdersOwnedByAnotherUser() {
        User otherUser = User.builder().id(2L).name("Other").email("other@example.com")
                .password("hash").role(Role.CUSTOMER).build();
        Order order = Order.builder().id(100L).user(otherUser).status(OrderStatus.CONFIRMED).build();
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.getMyOrder(1L, 100L))
                .isInstanceOf(com.bookstore.exception.ResourceNotFoundException.class)
                .hasMessageContaining("Order");
    }

    @Test
    void getMyOrdersRejectsInvalidUserId() {
        assertThatThrownBy(() -> service.getMyOrders(0L, PageRequest.of(0, 20)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID must be greater than zero");
        verifyNoInteractions(orderRepository);
    }
}
