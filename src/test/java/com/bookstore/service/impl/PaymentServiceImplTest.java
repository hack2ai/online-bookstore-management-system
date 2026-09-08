package com.bookstore.service.impl;

import com.bookstore.dto.request.PaymentVerifyRequest;
import com.bookstore.entity.Order;
import com.bookstore.entity.OrderStatus;
import com.bookstore.entity.Payment;
import com.bookstore.entity.PaymentStatus;
import com.bookstore.entity.User;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.repository.OrderRepository;
import com.bookstore.service.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CouponService couponService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        setField(paymentService, "paymentMode", "MOCK");
        setField(paymentService, "currency", "INR");
    }

    @Test
    void customerCannotCreatePaymentForAnotherCustomersOrder() {
        Order order = order(77L, 10L, OrderStatus.PENDING, new BigDecimal("499.00"));
        when(orderRepository.findById(77L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.createPayment(20L, 77L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found with id: 77");

        verify(orderRepository).findById(77L);
    }

    @Test
    void customerCannotVerifyPaymentForAnotherCustomersOrder() {
        Order order = order(77L, 10L, OrderStatus.PENDING, new BigDecimal("499.00"));
        order.setPayment(payment("mock-order-77", PaymentStatus.CREATED));
        when(orderRepository.findById(77L)).thenReturn(Optional.of(order));

        PaymentVerifyRequest request = PaymentVerifyRequest.builder()
                .razorpayOrderId("mock-order-77")
                .razorpayPaymentId("mock-payment-77")
                .razorpaySignature("signature")
                .build();

        assertThatThrownBy(() -> paymentService.verifyPayment(20L, 77L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found with id: 77");
    }

    @Test
    void verificationRejectsMismatchedPaymentOrderId() {
        Order order = order(77L, 20L, OrderStatus.PENDING, new BigDecimal("499.00"));
        order.setPayment(payment("mock-order-77", PaymentStatus.CREATED));
        when(orderRepository.findById(77L)).thenReturn(Optional.of(order));

        PaymentVerifyRequest request = PaymentVerifyRequest.builder()
                .razorpayOrderId("mock-order-999")
                .razorpayPaymentId("mock-payment-77")
                .razorpaySignature("signature")
                .build();

        assertThatThrownBy(() -> paymentService.verifyPayment(20L, 77L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Mock payment order ID does not match this order.");
    }

    @Test
    void cancelledOrderCannotCreatePayment() {
        Order order = order(77L, 20L, OrderStatus.CANCELLED, new BigDecimal("499.00"));
        when(orderRepository.findById(77L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.createPayment(20L, 77L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cancelled orders cannot be paid.");

        verify(orderRepository).findById(77L);
        verifyNoInteractions(couponService);
    }

    private Order order(Long id, Long userId, OrderStatus status, BigDecimal total) {
        User user = User.builder().id(userId).email("customer@example.com").build();
        return Order.builder()
                .id(id)
                .user(user)
                .status(status)
                .totalAmount(total)
                .subtotalAmount(total)
                .discountAmount(BigDecimal.ZERO)
                .build();
    }

    private Payment payment(String transactionId, PaymentStatus status) {
        return Payment.builder()
                .transactionId(transactionId)
                .paymentStatus(status)
                .build();
    }
}
