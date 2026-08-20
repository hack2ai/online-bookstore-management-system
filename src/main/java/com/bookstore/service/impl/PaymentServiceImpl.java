package com.bookstore.service.impl;

import com.bookstore.dto.request.PaymentVerifyRequest;
import com.bookstore.dto.response.PaymentResponse;
import com.bookstore.entity.Order;
import com.bookstore.entity.OrderStatus;
import com.bookstore.entity.Payment;
import com.bookstore.entity.PaymentStatus;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.repository.OrderRepository;
import com.bookstore.service.PaymentService;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Override
    @Transactional
    public PaymentResponse createPayment(Long userId, Long orderId) {
        Order order = findOwnedOrder(userId, orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled orders cannot be paid.");
        }

        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject options = new JSONObject();
            options.put("amount", order.getTotalAmount().movePointRight(2).longValueExact());
            options.put("currency", "INR");
            options.put("receipt", "BOOKSTORE-" + order.getId());
            com.razorpay.Order razorpayOrder = client.orders.create(options);

            Payment payment = order.getPayment();
            if (payment == null) {
                payment = Payment.builder().order(order).paymentMethod("RAZORPAY").build();
                order.setPayment(payment);
            }
            payment.setPaymentStatus(PaymentStatus.CREATED);
            payment.setTransactionId(razorpayOrder.get("id"));
            orderRepository.save(order);

            return PaymentResponse.builder().orderId(orderId)
                    .razorpayOrderId(razorpayOrder.get("id"))
                    .transactionId(payment.getTransactionId()).status(payment.getPaymentStatus()).build();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to create Razorpay payment order.", ex);
        }
    }

    @Override
    @Transactional
    public PaymentResponse verifyPayment(Long userId, Long orderId, PaymentVerifyRequest request) {
        Order order = findOwnedOrder(userId, orderId);
        Payment payment = order.getPayment();
        if (payment == null || payment.getTransactionId() == null) {
            throw new IllegalStateException("No payment has been created for this order.");
        }
        if (!payment.getTransactionId().equals(request.getRazorpayOrderId())) {
            throw new IllegalStateException("Payment order ID does not match this order.");
        }
        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return response(order, payment, request.getRazorpayOrderId());
        }

        try {
            JSONObject attributes = new JSONObject()
                    .put("razorpay_order_id", request.getRazorpayOrderId())
                    .put("razorpay_payment_id", request.getRazorpayPaymentId())
                    .put("razorpay_signature", request.getRazorpaySignature());
            com.razorpay.Utils.verifyPaymentSignature(attributes, keySecret);

            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(request.getRazorpayPaymentId());
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.CONFIRMED);
            }
            orderRepository.save(order);
            return response(order, payment, request.getRazorpayOrderId());
        } catch (Exception ex) {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
            throw new IllegalStateException("Payment signature verification failed.");
        }
    }

    private Order findOwnedOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Order", orderId);
        }
        return order;
    }

    private PaymentResponse response(Order order, Payment payment, String razorpayOrderId) {
        return PaymentResponse.builder().orderId(order.getId()).razorpayOrderId(razorpayOrderId)
                .transactionId(payment.getTransactionId()).status(payment.getPaymentStatus()).build();
    }
}
