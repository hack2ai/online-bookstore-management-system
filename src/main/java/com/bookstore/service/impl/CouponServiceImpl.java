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
    private final CouponRepository couponRepository;
    private final CouponUsageRepository usageRepository;

    @Override
    public DiscountResponse calculateDiscount(Long userId, String code, BigDecimal subtotal) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with code: " + code));
        return calculate(coupon, userId, subtotal);
    }

    @Override
    @Transactional
    public DiscountResponse calculateAndReserve(Long userId, String code, BigDecimal subtotal, User user) {
        Coupon coupon = couponRepository.findWithLockByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with code: " + code));
        DiscountResponse response = calculate(coupon, userId, subtotal);
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        usageRepository.save(CouponUsage.builder().coupon(coupon).user(user).build());
        return response;
    }

    @Override
    @Transactional
    public void releaseReservation(Long userId, String code) {
        if (code == null || code.isBlank()) return;
        Coupon coupon = couponRepository.findWithLockByCodeIgnoreCase(code.trim()).orElse(null);
        if (coupon == null) return;
        usageRepository.findByCouponIdAndUserId(coupon.getId(), userId).ifPresent(usage -> {
            usageRepository.delete(usage);
            coupon.setUsedCount(Math.max(0, coupon.getUsedCount() - 1));
        });
    }

    private DiscountResponse calculate(Coupon coupon, Long userId, BigDecimal subtotal) {
        LocalDateTime now = LocalDateTime.now();
        if (!coupon.isActive() || now.isBefore(coupon.getStartsAt()) || now.isAfter(coupon.getExpiresAt())) {
            throw new IllegalStateException("This coupon is not currently active.");
        }
        if (coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new IllegalStateException("This coupon has reached its usage limit.");
        }
        if (usageRepository.existsByCouponIdAndUserId(coupon.getId(), userId)) {
            throw new IllegalStateException("You have already used this coupon.");
        }
        if (subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new IllegalStateException("Minimum order value for this coupon is ₹" + coupon.getMinOrderAmount());
        }

        BigDecimal discount = coupon.getType() == CouponType.PERCENTAGE
                ? subtotal.multiply(coupon.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : coupon.getValue();
        if (coupon.getMaxDiscountAmount() != null) discount = discount.min(coupon.getMaxDiscountAmount());
        discount = discount.min(subtotal).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        return DiscountResponse.builder().code(coupon.getCode()).discount(discount)
                .originalSubtotal(subtotal).discountedSubtotal(subtotal.subtract(discount)).build();
    }
}
