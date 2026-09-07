package com.bookstore.service.impl;

import com.bookstore.dto.response.DiscountResponse;
import com.bookstore.entity.Coupon;
import com.bookstore.entity.CouponType;
import com.bookstore.entity.CouponUsage;
import com.bookstore.entity.User;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.repository.CouponRepository;
import com.bookstore.repository.CouponUsageRepository;
import com.bookstore.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponServiceImpl implements CouponService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final CouponRepository couponRepository;
    private final CouponUsageRepository usageRepository;

    @Override
    public DiscountResponse calculateDiscount(Long userId, String code, BigDecimal subtotal) {
        validateUserId(userId);
        validateSubtotal(subtotal);
        String normalizedCode = normalizeCode(code);
        Coupon coupon = couponRepository.findByCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with code: " + normalizedCode));
        return calculate(coupon, userId, subtotal);
    }

    @Override
    @Transactional
    public DiscountResponse calculateAndReserve(Long userId, String code, BigDecimal subtotal, User user) {
        validateUserId(userId);
        validateSubtotal(subtotal);
        if (user == null || user.getId() == null || !userId.equals(user.getId())) {
            throw new IllegalArgumentException("Coupon user context is invalid.");
        }

        String normalizedCode = normalizeCode(code);
        Coupon coupon = couponRepository.findWithLockByCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with code: " + normalizedCode));

        DiscountResponse response = calculate(coupon, userId, subtotal);
        int usedCount = nonNegativeInt(coupon.getUsedCount());
        int usageLimit = positiveInt(coupon.getUsageLimit());
        if (usedCount >= usageLimit) {
            throw new IllegalStateException("This coupon has reached its usage limit.");
        }

        coupon.setUsedCount(usedCount + 1);
        usageRepository.save(CouponUsage.builder().coupon(coupon).user(user).build());
        return response;
    }

    @Override
    @Transactional
    public void releaseReservation(Long userId, String code) {
        if (userId == null || userId <= 0 || code == null || code.isBlank()) {
            return;
        }

        Coupon coupon = couponRepository.findWithLockByCodeIgnoreCase(code.trim()).orElse(null);
        if (coupon == null) {
            return;
        }

        usageRepository.findByCouponIdAndUserId(coupon.getId(), userId).ifPresent(usage -> {
            usageRepository.delete(usage);
            coupon.setUsedCount(Math.max(0, nonNegativeInt(coupon.getUsedCount()) - 1));
        });
    }

    private DiscountResponse calculate(Coupon coupon, Long userId, BigDecimal subtotal) {
        if (coupon == null) {
            throw new IllegalStateException("Coupon is invalid.");
        }
        if (coupon.getType() == null || coupon.getValue() == null || coupon.getMinOrderAmount() == null
                || coupon.getStartsAt() == null || coupon.getExpiresAt() == null) {
            throw new IllegalStateException("Coupon configuration is incomplete.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (!coupon.isActive() || now.isBefore(coupon.getStartsAt()) || now.isAfter(coupon.getExpiresAt())) {
            throw new IllegalStateException("This coupon is not currently active.");
        }
        if (!coupon.getExpiresAt().isAfter(coupon.getStartsAt())) {
            throw new IllegalStateException("Coupon validity period is invalid.");
        }

        int usedCount = nonNegativeInt(coupon.getUsedCount());
        int usageLimit = positiveInt(coupon.getUsageLimit());
        if (usedCount >= usageLimit) {
            throw new IllegalStateException("This coupon has reached its usage limit.");
        }
        if (usageRepository.existsByCouponIdAndUserId(coupon.getId(), userId)) {
            throw new IllegalStateException("You have already used this coupon.");
        }

        BigDecimal minimumOrder = nonNegative(coupon.getMinOrderAmount());
        if (subtotal.compareTo(minimumOrder) < 0) {
            throw new IllegalStateException("Minimum order value for this coupon is ₹" + minimumOrder);
        }

        BigDecimal value = nonNegative(coupon.getValue());
        BigDecimal discount;
        if (coupon.getType() == CouponType.PERCENTAGE) {
            if (value.compareTo(ONE_HUNDRED) > 0) {
                throw new IllegalStateException("Coupon percentage cannot exceed 100%.");
            }
            discount = subtotal.multiply(value)
                    .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
        } else {
            discount = value;
        }

        if (coupon.getMaxDiscountAmount() != null) {
            discount = discount.min(nonNegative(coupon.getMaxDiscountAmount()));
        }
        discount = discount.min(subtotal).max(ZERO).setScale(2, RoundingMode.HALF_UP);

        return DiscountResponse.builder()
                .code(coupon.getCode())
                .discount(discount)
                .originalSubtotal(subtotal)
                .discountedSubtotal(subtotal.subtract(discount).setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Coupon code is required.");
        }
        String normalized = code.trim();
        if (normalized.length() > 40) {
            throw new IllegalArgumentException("Coupon code must not exceed 40 characters.");
        }
        return normalized;
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be greater than zero");
        }
    }

    private void validateSubtotal(BigDecimal subtotal) {
        if (subtotal == null || subtotal.signum() < 0) {
            throw new IllegalArgumentException("Subtotal must be zero or greater.");
        }
    }

    private BigDecimal nonNegative(BigDecimal amount) {
        return amount == null || amount.signum() < 0 ? ZERO : amount;
    }

    private int nonNegativeInt(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    private int positiveInt(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }
}
