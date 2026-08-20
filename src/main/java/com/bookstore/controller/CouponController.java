package com.bookstore.controller;

import com.bookstore.dto.request.CouponApplyRequest;
import com.bookstore.dto.response.ApiResponse;
import com.bookstore.dto.response.DiscountResponse;
import com.bookstore.security.CustomUserDetails;
import com.bookstore.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CouponController {
    private final CouponService couponService;

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<DiscountResponse>> validate(
            Authentication authentication,
            @RequestParam BigDecimal subtotal,
            @Valid @RequestBody CouponApplyRequest request) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
        return ResponseEntity.ok(ApiResponse.success("Coupon validated successfully.",
                couponService.calculateDiscount(userId, request.getCode(), subtotal)));
    }
}
