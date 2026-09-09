package com.bookstore.service;

import com.bookstore.dto.request.PaymentVerifyRequest;
import com.bookstore.dto.response.PaymentResponse;
import com.bookstore.entity.Order;
import com.bookstore.entity.OrderStatus;
import com.bookstore.entity.Payment;
import com.bookstore.entity.PaymentStatus;
import com.bookstore.entity.Role;
import com.bookstore.entity.User;
import com.bookstore.repository.OrderRepository;
import com.bookstore.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock OrderRepository orderRepository;
    @Mock CouponService couponService;

    private PaymentServiceImpl service;
    private Order order;

    @BeforeEach
    void setUp() {
        service = new PaymentServiceImpl(orderRepository, couponService);
        ReflectionTestUtils.setField(service, "paymentMode", "RAZORPAY");
        ReflectionTestUtils.setField(service, "keyId", "test-key");
        ReflectionTestUtils.setField(service, "keySecret", "test-secret");

        User user = User.builder().id(1L).name("Test").email("test@example.com")
                .password("hash").role(Role.CUSTOMER).build();
        order = Order.builder().id(100L).user(user).subtotalAmount(new BigDecimal("800.00"))
                .discountAmount(BigDecimal.ZERO).totalAmount(new BigDecimal("800.00"))
                .shippingAddress("Bengaluru").status(OrderStatus.PENDING).build();
        Payment payment = Payment.builder().order(order).paymentMethod("RAZORPAY")
                .paymentStatus(PaymentStatus.SUCCESS)
                .providerOrderId("order_123")
                .transactionId("pay_123")
                .build();
        order.setPayment(payment);
    }

    @Test
    void alreadySuccessfulPaymentIsIdempotent() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        PaymentResponse response = service.verifyPayment(1L, 100L,
                PaymentVerifyRequest.builder().razorpayOrderId("order_123")
                        .razorpayPaymentId("pay_123").razorpaySignature("signature").build());

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getRazorpayOrderId()).isEqualTo("order_123");
        assertThat(response.getTransactionId()).isEqualTo("pay_123");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelledOrderCannotCreatePayment() {
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.createPayment(1L, 100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cancelled orders cannot be paid");
    }

    @Test
    void invalidUserIdIsRejectedBeforeRepositoryAccess() {
        assertThatThrownBy(() -> service.createPayment(0L, 100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID must be greater than zero");

        verifyNoInteractions(orderRepository);
    }

    @Test
    void invalidOrderIdIsRejectedBeforeRepositoryAccess() {
        assertThatThrownBy(() -> service.createPayment(1L, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order ID must be greater than zero");

        verifyNoInteractions(orderRepository);
    }

    @Test
    void paymentVerificationRequiresAllFields() {
        PaymentVerifyRequest request = PaymentVerifyRequest.builder()
                .razorpayOrderId("order_123")
                .razorpayPaymentId("")
                .razorpaySignature("signature")
                .build();

        assertThatThrownBy(() -> service.verifyPayment(1L, 100L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment verification details are required");

        verifyNoInteractions(orderRepository);
    }

    @Test
    void paymentCannotBeCreatedWhenOrderAmountIsInvalid() {
        order.setPayment(null);
        order.setTotalAmount(new BigDecimal("-1.00"));
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.createPayment(1L, 100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid payment amount");
    }
}
