package com.bookstore.controller;

import com.bookstore.dto.request.PaymentVerifyRequest;
import com.bookstore.dto.response.ApiResponse;
import com.bookstore.dto.response.PaymentResponse;
import com.bookstore.security.CustomUserDetails;
import com.bookstore.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Razorpay payment lifecycle")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/orders/{orderId}")
    @Operation(summary = "Create a Razorpay payment order")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            Authentication authentication, @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success("Payment order created successfully.",
                paymentService.createPayment(currentUserId(authentication), orderId)));
    }

    @PostMapping("/orders/{orderId}/verify")
    @Operation(summary = "Verify a Razorpay payment signature")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            Authentication authentication,
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentVerifyRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payment verified successfully.",
                paymentService.verifyPayment(currentUserId(authentication), orderId, request)));
    }

    private Long currentUserId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
    }
}
