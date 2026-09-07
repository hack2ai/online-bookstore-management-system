package com.bookstore.controller;

import com.bookstore.dto.request.CouponApplyRequest;
import com.bookstore.dto.response.ApiResponse;
import com.bookstore.dto.response.DiscountResponse;
import com.bookstore.security.CustomUserDetails;
import com.bookstore.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse as OpenApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Coupons", description = "Customer coupon validation")
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/validate")
    @Operation(summary = "Validate a coupon", description = "Validates a coupon for the authenticated customer without reserving or consuming it.")
    @ApiResponses({
            @OpenApiResponse(responseCode = "200", description = "Coupon validated successfully"),
            @OpenApiResponse(responseCode = "400", description = "Invalid subtotal, coupon code, or coupon eligibility"),
            @OpenApiResponse(responseCode = "401", description = "Authentication required"),
            @OpenApiResponse(responseCode = "403", description = "Customer role required")
    })
    public ResponseEntity<ApiResponse<DiscountResponse>> validate(
            Authentication authentication,
            @Parameter(name = "subtotal", in = ParameterIn.QUERY, description = "Order subtotal before discount", required = true)
            @RequestParam BigDecimal subtotal,
            @Valid @RequestBody CouponApplyRequest request) {
        Long userId = currentUserId(authentication);
        DiscountResponse response = couponService.calculateDiscount(userId, request.getCode(), subtotal);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.success("Coupon validated successfully.", response));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails details)
                || details.getUser() == null || details.getUser().getId() == null) {
            throw new IllegalStateException("Authenticated user context is unavailable.");
        }
        return details.getUser().getId();
    }
}
