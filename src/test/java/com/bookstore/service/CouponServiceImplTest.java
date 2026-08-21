package com.bookstore.service;

import com.bookstore.dto.response.DiscountResponse;
import com.bookstore.entity.Coupon;
import com.bookstore.entity.CouponType;
import com.bookstore.entity.User;
import com.bookstore.repository.CouponRepository;
import com.bookstore.repository.CouponUsageRepository;
import com.bookstore.service.impl.CouponServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    @Mock CouponRepository couponRepository;
    @Mock CouponUsageRepository usageRepository;

    private CouponServiceImpl service;
    private final User user = User.builder().id(1L).name("Test").email("test@example.com").password("hash").build();

    @BeforeEach
    void setUp() {
        service = new CouponServiceImpl(couponRepository, usageRepository);
    }

    @Test
    void percentageCouponAppliesDiscount() {
        Coupon coupon = validCoupon(CouponType.PERCENTAGE, "20.00");
        when(couponRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(coupon));
        when(usageRepository.existsByCouponIdAndUserId(10L, 1L)).thenReturn(false);

        DiscountResponse result = service.calculateDiscount(1L, "SAVE20", new BigDecimal("1000.00"));

        assertThat(result.getDiscount()).isEqualByComparingTo("200.00");
        assertThat(result.getDiscountedSubtotal()).isEqualByComparingTo("800.00");
    }

    @Test
    void maximumDiscountCapsPercentageCoupon() {
        Coupon coupon = validCoupon(CouponType.PERCENTAGE, "50.00");
        coupon.setMaxDiscountAmount(new BigDecimal("100.00"));
        when(couponRepository.findByCodeIgnoreCase("HALF")).thenReturn(Optional.of(coupon));
        when(usageRepository.existsByCouponIdAndUserId(10L, 1L)).thenReturn(false);

        DiscountResponse result = service.calculateDiscount(1L, "HALF", new BigDecimal("1000.00"));

        assertThat(result.getDiscount()).isEqualByComparingTo("100.00");
    }

    @Test
    void usedCouponIsRejected() {
        Coupon coupon = validCoupon(CouponType.FIXED, "100.00");
        when(couponRepository.findByCodeIgnoreCase("ONCE")).thenReturn(Optional.of(coupon));
        when(usageRepository.existsByCouponIdAndUserId(10L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.calculateDiscount(1L, "ONCE", new BigDecimal("1000.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already used");
    }

    @Test
    void expiredCouponIsRejected() {
        Coupon coupon = validCoupon(CouponType.FIXED, "100.00");
        coupon.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(couponRepository.findByCodeIgnoreCase("OLD")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> service.calculateDiscount(1L, "OLD", new BigDecimal("1000.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not currently active");
    }

    private Coupon validCoupon(CouponType type, String value) {
        return Coupon.builder().id(10L).code("TEST").type(type).value(new BigDecimal(value))
                .minOrderAmount(new BigDecimal("500.00")).maxDiscountAmount(null)
                .usageLimit(10).usedCount(0).active(true)
                .startsAt(LocalDateTime.now().minusHours(1)).expiresAt(LocalDateTime.now().plusHours(1)).build();
    }
}
