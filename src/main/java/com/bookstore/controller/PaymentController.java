package com.bookstore.controller;

import com.bookstore.dto.request.PaymentVerifyRequest;
import com.bookstore.dto.response.ApiResponse;
import com.bookstore.dto.response.PaymentResponse;
import com.bookstore.security.CustomUserDetails;
import com.bookstore.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Secure payment lifecycle for customer orders")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/orders/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Create a payment order", description = "Creates or reuses the payment provider order for the authenticated customer's order.")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            Authentication authentication,
            @Parameter(name = "orderId", in = ParameterIn.PATH, description = "Order ID", required = true)
            @PathVariable Long orderId) {
        validateOrderId(orderId);
        PaymentResponse response = paymentService.createPayment(currentUserId(authentication), orderId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.success("Payment order created successfully.", response));
    }

    @PostMapping("/orders/{orderId}/verify")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Verify a payment", description = "Verifies the payment provider signature for the authenticated customer's order.")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            Authentication authentication,
            @Parameter(name = "orderId", in = ParameterIn.PATH, description = "Order ID", required = true)
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentVerifyRequest request) {
        validateOrderId(orderId);
        PaymentResponse response = paymentService.verifyPayment(currentUserId(authentication), orderId, request);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.success("Payment verified successfully.", response));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails details)
                || details.getUser() == null || details.getUser().getId() == null) {
            throw new IllegalStateException("Authenticated user context is unavailable.");
        }
        return details.getUser().getId();
    }

    private void validateOrderId(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Order ID must be greater than zero.");
        }
    }
}
