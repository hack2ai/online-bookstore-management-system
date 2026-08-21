package com.bookstore.controller;

import com.bookstore.dto.request.PaymentVerifyRequest;
import com.bookstore.dto.response.ApiResponse;
import com.bookstore.dto.response.OrderResponse;
import com.bookstore.dto.response.PaymentResponse;
import com.bookstore.security.CustomUserDetails;
import com.bookstore.service.OrderService;
import com.bookstore.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
@RequiredArgsConstructor
public class PaymentPageController {
    private final OrderService orderService;
    private final PaymentService paymentService;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @GetMapping("/orders/{orderId}/payment")
    public String payment(@PathVariable Long orderId, Authentication authentication, Model model) {
        Long userId = currentUserId(authentication);
        OrderResponse order = orderService.getMyOrder(userId, orderId);
        model.addAttribute("order", order);
        model.addAttribute("razorpayKeyId", razorpayKeyId);
        return "payment";
    }

    @PostMapping("/orders/{orderId}/payment/create")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @PathVariable Long orderId,
            Authentication authentication) {
        PaymentResponse payment = paymentService.createPayment(currentUserId(authentication), orderId);
        return ResponseEntity.ok(ApiResponse.success("Payment order created successfully.", payment));
    }

    @PostMapping("/orders/{orderId}/payment/verify")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @PathVariable Long orderId,
            Authentication authentication,
            @Valid @RequestBody PaymentVerifyRequest request) {
        PaymentResponse payment = paymentService.verifyPayment(currentUserId(authentication), orderId, request);
        return ResponseEntity.ok(ApiResponse.success("Payment verified successfully.", payment));
    }

    private Long currentUserId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
    }
}
