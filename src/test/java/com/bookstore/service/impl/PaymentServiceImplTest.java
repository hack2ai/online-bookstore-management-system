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

import static org.assertj.core.api.Assertions.assertThat;
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
    void mockCreatePaymentStoresProviderOrderIdAndLeavesTransactionUnset() {
        Order order = order(77L, 20L, OrderStatus.PENDING, new BigDecimal("499.00"));
        when(orderRepository.findById(77L)).thenReturn(Optional.of(order));

        var response = paymentService.createPayment(20L, 77L);

        assertThat(response.getRazorpayOrderId()).isEqualTo("mock-order-77");
        assertThat(response.getTransactionId()).isNull();
        assertThat(order.getPayment().getProviderOrderId()).isEqualTo("mock-order-77");
        assertThat(order.getPayment().getTransactionId()).isNull();
        assertThat(order.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.CREATED);
        verify(orderRepository).save(order);
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

        PaymentVerifyRequest request = verifyRequest("mock-order-77");

        assertThatThrownBy(() -> paymentService.verifyPayment(20L, 77L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found with id: 77");
    }

    @Test
    void verificationRejectsMismatchedPaymentOrderId() {
        Order order = order(77L, 20L, OrderStatus.PENDING, new BigDecimal("499.00"));
        order.setPayment(payment("mock-order-77", PaymentStatus.CREATED));
        when(orderRepository.findById(77L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.verifyPayment(20L, 77L, verifyRequest("mock-order-999")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Mock payment order ID does not match this order.");
    }

    @Test
    void successfulMockVerificationSeparatesOrderAndTransactionIds() {
        Order order = order(77L, 20L, OrderStatus.PENDING, new BigDecimal("499.00"));
        order.setPayment(payment("mock-order-77", PaymentStatus.CREATED));
        when(orderRepository.findById(77L)).thenReturn(Optional.of(order));

        var response = paymentService.verifyPayment(20L, 77L, verifyRequest("mock-order-77"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getPayment().getProviderOrderId()).isEqualTo("mock-order-77");
        assertThat(order.getPayment().getTransactionId()).isEqualTo("mock-payment-77");
        assertThat(order.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getRazorpayOrderId()).isEqualTo("mock-order-77");
        assertThat(response.getTransactionId()).isEqualTo("mock-payment-77");
    }

    @Test
    void repeatedCreatePaymentReusesExistingProviderOrderId() {
        Order order = order(77L, 20L, OrderStatus.PENDING, new BigDecimal("499.00"));
        order.setPayment(payment("mock-order-77", PaymentStatus.CREATED));
        when(orderRepository.findById(77L)).thenReturn(Optional.of(order));

        var response = paymentService.createPayment(20L, 77L);

        assertThat(response.getRazorpayOrderId()).isEqualTo("mock-order-77");
        assertThat(response.getTransactionId()).isNull();
        verifyNoInteractions(couponService);
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

    private Payment payment(String providerOrderId, PaymentStatus status) {
        return Payment.builder()
                .providerOrderId(providerOrderId)
                .paymentStatus(status)
                .build();
    }

    private PaymentVerifyRequest verifyRequest(String orderId) {
        return PaymentVerifyRequest.builder()
                .razorpayOrderId(orderId)
                .razorpayPaymentId("mock-payment-77")
                .razorpaySignature("signature")
                .build();
    }
}
