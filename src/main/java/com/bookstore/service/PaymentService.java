package com.bookstore.service;

import com.bookstore.dto.request.PaymentVerifyRequest;
import com.bookstore.dto.response.PaymentResponse;

public interface PaymentService {
    PaymentResponse createPayment(Long userId, Long orderId);
    PaymentResponse verifyPayment(Long userId, Long orderId, PaymentVerifyRequest request);
}
