package com.bookstore.controller;

import com.bookstore.dto.request.CouponAdminRequest;
import com.bookstore.dto.response.ApiResponse;
import com.bookstore.dto.response.CouponAdminResponse;
import com.bookstore.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Coupons", description = "Administrative coupon lifecycle management")
public class AdminCouponController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private static final java.util.Set<String> ALLOWED_SORTS = java.util.Set.of(
            "code", "type", "value", "minOrderAmount", "usageLimit", "usedCount", "startsAt", "expiresAt", "active"
    );

    private final CouponService couponService;

    @GetMapping
    @Operation(summary = "List coupons", description = "Returns a paginated administrative view of coupons, optionally filtered by active status.")
    public ResponseEntity<ApiResponse<Page<CouponAdminResponse>>> getAll(
            @Parameter(name = "active", in = ParameterIn.QUERY, description = "Filter by active status")
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                "Coupons retrieved successfully.",
                couponService.getAdminCoupons(active, sanitizePageable(pageable))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a coupon", description = "Returns a single coupon for administrative inspection.")
    public ResponseEntity<ApiResponse<CouponAdminResponse>> getById(
            @Parameter(name = "id", in = ParameterIn.PATH, description = "Coupon ID", required = true)
            @PathVariable Long id) {
        validateId(id);
        return ResponseEntity.ok(ApiResponse.success(
                "Coupon retrieved successfully.", couponService.getAdminCoupon(id)));
    }

    @PostMapping
    @Operation(summary = "Create a coupon", description = "Creates a validated coupon and starts with zero reserved/consumed uses.")
    public ResponseEntity<ApiResponse<CouponAdminResponse>> create(
            @Valid @RequestBody CouponAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Coupon created successfully.", couponService.createAdminCoupon(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a coupon", description = "Updates coupon configuration while preserving its usage history and enforcing usage-limit safety.")
    public ResponseEntity<ApiResponse<CouponAdminResponse>> update(
            @Parameter(name = "id", in = ParameterIn.PATH, description = "Coupon ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody CouponAdminRequest request) {
        validateId(id);
        return ResponseEntity.ok(ApiResponse.success(
                "Coupon updated successfully.", couponService.updateAdminCoupon(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an unused coupon", description = "Deletes a coupon only when it has never been reserved or consumed.")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(name = "id", in = ParameterIn.PATH, description = "Coupon ID", required = true)
            @PathVariable Long id) {
        validateId(id);
        couponService.deleteAdminCoupon(id);
        return ResponseEntity.ok(ApiResponse.success("Coupon deleted successfully."));
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Coupon ID must be greater than zero");
        }
    }

    private Pageable sanitizePageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, DEFAULT_PAGE_SIZE, Sort.by("code").ascending());
        }
        int page = Math.max(pageable.getPageNumber(), 0);
        int size = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        Sort safeSort = pageable.getSort().stream()
                .filter(order -> ALLOWED_SORTS.contains(order.getProperty()))
                .map(order -> new Sort.Order(
                        order.getDirection(), order.getProperty(), order.getNullHandling(), order.isIgnoreCase()))
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(),
                        orders -> orders.isEmpty() ? Sort.by("code").ascending() : Sort.by(orders)));
        return PageRequest.of(page, size, safeSort);
    }
}
