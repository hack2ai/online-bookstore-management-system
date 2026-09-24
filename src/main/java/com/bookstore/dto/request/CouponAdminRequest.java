package com.bookstore.dto.request;

import com.bookstore.entity.CouponType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponAdminRequest {

    @NotBlank(message = "Coupon code is required")
    @Size(min = 3, max = 40, message = "Coupon code must be between 3 and 40 characters")
    private String code;

    @NotNull(message = "Coupon type is required")
    private CouponType type;

    @NotNull(message = "Coupon value is required")
    @DecimalMin(value = "0.00", message = "Coupon value must be zero or greater")
    private BigDecimal value;

    @NotNull(message = "Minimum order amount is required")
    @DecimalMin(value = "0.00", message = "Minimum order amount must be zero or greater")
    private BigDecimal minOrderAmount;

    @DecimalMin(value = "0.00", message = "Maximum discount amount must be zero or greater")
    private BigDecimal maxDiscountAmount;

    @NotNull(message = "Usage limit is required")
    @Min(value = 1, message = "Usage limit must be at least 1")
    private Integer usageLimit;

    @NotNull(message = "Start time is required")
    private LocalDateTime startsAt;

    @NotNull(message = "Expiry time is required")
    private LocalDateTime expiresAt;

    @Builder.Default
    private boolean active = true;
}
