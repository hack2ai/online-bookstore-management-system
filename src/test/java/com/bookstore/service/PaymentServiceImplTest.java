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

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock OrderRepository orderRepository;

    private PaymentServiceImpl service;
    private Order order;

    @BeforeEach
    void setUp() {
        service = new PaymentServiceImpl(orderRepository);

        User user = User.builder().id(1L).name("Test").email("test@example.com")
                .password("hash").role(Role.CUSTOMER).build();
        order = Order.builder().id(100L).user(user).totalAmount(new BigDecimal("800.00"))
                .shippingAddress("Bengaluru").status(OrderStatus.PENDING).build();
        Payment payment = Payment.builder().order(order).paymentMethod("RAZORPAY")
                .paymentStatus(PaymentStatus.SUCCESS).transactionId("order_123").build();
        order.setPayment(payment);
    }

    @Test
    void alreadySuccessfulPaymentIsIdempotent() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        PaymentResponse response = service.verifyPayment(1L, 100L,
                PaymentVerifyRequest.builder().razorpayOrderId("order_123")
                        .razorpayPaymentId("pay_123").razorpaySignature("signature").build());

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
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
}
