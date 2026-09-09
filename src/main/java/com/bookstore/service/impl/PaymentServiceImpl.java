package com.bookstore.service.impl;

import com.bookstore.dto.request.PaymentVerifyRequest;
import com.bookstore.dto.response.PaymentResponse;
import com.bookstore.entity.Order;
import com.bookstore.entity.OrderStatus;
import com.bookstore.entity.Payment;
import com.bookstore.entity.PaymentStatus;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.repository.OrderRepository;
import com.bookstore.service.CouponService;
import com.bookstore.service.PaymentService;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final String MOCK_MODE = "MOCK";
    private static final String RAZORPAY_MODE = "RAZORPAY";
    private static final String MOCK_METHOD = "MOCK";
    private static final String RAZORPAY_METHOD = "RAZORPAY";

    private final OrderRepository orderRepository;
    private final CouponService couponService;

    @Value("${razorpay.key-id:}")
    private String keyId;

    @Value("${razorpay.key-secret:}")
    private String keySecret;

    @Value("${razorpay.currency:INR}")
    private String currency;

    @Value("${payment.mode:RAZORPAY}")
    private String paymentMode;

    @Override
    @Transactional
    public PaymentResponse createPayment(Long userId, Long orderId) {
        validateId(userId, "User ID");
        validateId(orderId, "Order ID");
        Order order = findOwnedOrder(userId, orderId);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled orders cannot be paid.");
        }
        validateOrderAmount(order);
        String mode = normalizedMode();

        Payment payment = order.getPayment();
        if (payment != null && payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return response(order, payment, payment.getProviderOrderId());
        }
        if (payment != null && payment.getPaymentStatus() == PaymentStatus.CREATED
                && payment.getProviderOrderId() != null && !payment.getProviderOrderId().isBlank()) {
            return response(order, payment, payment.getProviderOrderId());
        }

        if (MOCK_MODE.equals(mode)) {
            if (payment == null) {
                payment = Payment.builder().order(order).paymentMethod(MOCK_METHOD).build();
                order.setPayment(payment);
            } else {
                payment.setPaymentMethod(MOCK_METHOD);
            }
            payment.setPaymentStatus(PaymentStatus.CREATED);
            payment.setProviderOrderId("mock-order-" + order.getId());
            payment.setTransactionId(null);
            orderRepository.save(order);
            return response(order, payment, payment.getProviderOrderId());
        }

        requireRazorpayConfiguration();

        try {
            RazorpayClient client = new RazorpayClient(keyId.trim(), keySecret.trim());
            JSONObject options = new JSONObject()
                    .put("amount", order.getTotalAmount().setScale(2).movePointRight(2).longValueExact())
                    .put("currency", normalizedCurrency())
                    .put("receipt", "BOOKSTORE-" + order.getId());

            com.razorpay.Order razorpayOrder = client.orders.create(options);
            String razorpayOrderId = razorpayOrder.get("id");
            if (razorpayOrderId == null || razorpayOrderId.isBlank()) {
                throw new IllegalStateException("Razorpay did not return a payment order ID.");
            }

            if (payment == null) {
                payment = Payment.builder().order(order).paymentMethod(RAZORPAY_METHOD).build();
                order.setPayment(payment);
            } else {
                payment.setPaymentMethod(RAZORPAY_METHOD);
            }
            payment.setPaymentStatus(PaymentStatus.CREATED);
            payment.setProviderOrderId(razorpayOrderId);
            payment.setTransactionId(null);
            orderRepository.save(order);

            return response(order, payment, razorpayOrderId);
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to create Razorpay payment order.");
        }
    }

    @Override
    @Transactional
    public PaymentResponse verifyPayment(Long userId, Long orderId, PaymentVerifyRequest request) {
        validateId(userId, "User ID");
        validateId(orderId, "Order ID");
        validateVerifyRequest(request);

        Order order = findOwnedOrder(userId, orderId);
        Payment payment = order.getPayment();
        if (payment == null || payment.getProviderOrderId() == null || payment.getProviderOrderId().isBlank()) {
            throw new IllegalStateException("No payment has been created for this order.");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled orders cannot be paid.");
        }

        String mode = normalizedMode();
        if (MOCK_MODE.equals(mode)) {
            return verifyMockPayment(order, payment, request);
        }

        requireRazorpayConfiguration();

        String requestedOrderId = request.getRazorpayOrderId().trim();
        if (!payment.getProviderOrderId().equals(requestedOrderId)) {
            throw new IllegalStateException("Payment order ID does not match this order.");
        }
        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return response(order, payment, requestedOrderId);
        }

        try {
            JSONObject attributes = new JSONObject()
                    .put("razorpay_order_id", requestedOrderId)
                    .put("razorpay_payment_id", request.getRazorpayPaymentId().trim())
                    .put("razorpay_signature", request.getRazorpaySignature().trim());
            com.razorpay.Utils.verifyPaymentSignature(attributes, keySecret.trim());

            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(request.getRazorpayPaymentId().trim());
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.CONFIRMED);
            }
            orderRepository.save(order);
            return response(order, payment, requestedOrderId);
        } catch (Exception ex) {
            markPaymentFailed(order, payment, userId);
            throw new IllegalStateException("Payment signature verification failed.");
        }
    }

    private PaymentResponse verifyMockPayment(Order order, Payment payment, PaymentVerifyRequest request) {
        String requestedOrderId = request.getRazorpayOrderId().trim();
        if (!payment.getProviderOrderId().equals(requestedOrderId)) {
            throw new IllegalStateException("Mock payment order ID does not match this order.");
        }
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setTransactionId("mock-payment-" + order.getId());
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
        }
        orderRepository.save(order);
        return response(order, payment, requestedOrderId);
    }

    private void markPaymentFailed(Order order, Payment payment, Long userId) {
        payment.setPaymentStatus(PaymentStatus.FAILED);
        orderRepository.save(order);
        if (order.getStatus() == OrderStatus.PENDING && order.getCouponCode() != null) {
            couponService.releaseReservation(userId, order.getCouponCode());
            order.setCouponCode(null);
            order.setDiscountAmount(BigDecimal.ZERO);
            order.setTotalAmount(nonNegative(order.getSubtotalAmount()));
            orderRepository.save(order);
        }
    }

    private Order findOwnedOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (order.getUser() == null || order.getUser().getId() == null
                || !order.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Order", orderId);
        }
        return order;
    }

    private PaymentResponse response(Order order, Payment payment, String providerOrderId) {
        return PaymentResponse.builder()
                .orderId(order.getId())
                .razorpayOrderId(providerOrderId)
                .transactionId(payment.getTransactionId())
                .status(payment.getPaymentStatus())
                .build();
    }

    private void validateOrderAmount(Order order) {
        BigDecimal total = order.getTotalAmount();
        if (total == null || total.signum() < 0) {
            throw new IllegalStateException("Order has an invalid payment amount.");
        }
        if (total.movePointRight(2).compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0) {
            throw new IllegalStateException("Order amount is too large for payment processing.");
        }
    }

    private void validateVerifyRequest(PaymentVerifyRequest request) {
        if (request == null || isBlank(request.getRazorpayOrderId())
                || isBlank(request.getRazorpayPaymentId()) || isBlank(request.getRazorpaySignature())) {
            throw new IllegalArgumentException("Payment verification details are required.");
        }
    }

    private void validateId(Long id, String name) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(name + " must be greater than zero");
        }
    }

    private void requireRazorpayConfiguration() {
        if (isBlank(keyId) || isBlank(keySecret)) {
            throw new IllegalStateException("Razorpay payment configuration is missing.");
        }
    }

    private String normalizedMode() {
        String mode = paymentMode == null ? RAZORPAY_MODE : paymentMode.trim().toUpperCase();
        if (!MOCK_MODE.equals(mode) && !RAZORPAY_MODE.equals(mode)) {
            throw new IllegalStateException("Unsupported payment mode: " + mode);
        }
        return mode;
    }

    private String normalizedCurrency() {
        String value = currency == null ? "INR" : currency.trim().toUpperCase();
        return value.isEmpty() ? "INR" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private BigDecimal nonNegative(BigDecimal amount) {
        return amount == null || amount.signum() < 0 ? BigDecimal.ZERO : amount;
    }
}
