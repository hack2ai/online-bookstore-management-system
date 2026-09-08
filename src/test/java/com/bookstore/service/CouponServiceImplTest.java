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
import static org.mockito.ArgumentMatchers.*;
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
    void fixedCouponAppliesDiscount() {
        Coupon coupon = validCoupon(CouponType.FIXED, "150.00");
        when(couponRepository.findByCodeIgnoreCase("FIXED150")).thenReturn(Optional.of(coupon));
        when(usageRepository.existsByCouponIdAndUserId(10L, 1L)).thenReturn(false);

        DiscountResponse result = service.calculateDiscount(1L, "FIXED150", new BigDecimal("1000.00"));

        assertThat(result.getDiscount()).isEqualByComparingTo("150.00");
        assertThat(result.getDiscountedSubtotal()).isEqualByComparingTo("850.00");
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

    @Test
    void inactiveCouponIsRejected() {
        Coupon coupon = validCoupon(CouponType.FIXED, "100.00");
        coupon.setActive(false);
        when(couponRepository.findByCodeIgnoreCase("OFF")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> service.calculateDiscount(1L, "OFF", new BigDecimal("1000.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not currently active");
    }

    @Test
    void couponOutsideMinimumOrderIsRejected() {
        Coupon coupon = validCoupon(CouponType.FIXED, "100.00");
        when(couponRepository.findByCodeIgnoreCase("MIN500")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> service.calculateDiscount(1L, "MIN500", new BigDecimal("499.99")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Minimum order value");
    }

    @Test
    void percentageAboveHundredIsRejected() {
        Coupon coupon = validCoupon(CouponType.PERCENTAGE, "101.00");
        when(couponRepository.findByCodeIgnoreCase("BADPERCENT")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> service.calculateDiscount(1L, "BADPERCENT", new BigDecimal("1000.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot exceed 100%");
    }

    @Test
    void discountNeverExceedsSubtotal() {
        Coupon coupon = validCoupon(CouponType.FIXED, "2000.00");
        when(couponRepository.findByCodeIgnoreCase("BIGFIXED")).thenReturn(Optional.of(coupon));
        when(usageRepository.existsByCouponIdAndUserId(10L, 1L)).thenReturn(false);

        DiscountResponse result = service.calculateDiscount(1L, "BIGFIXED", new BigDecimal("1000.00"));

        assertThat(result.getDiscount()).isEqualByComparingTo("1000.00");
        assertThat(result.getDiscountedSubtotal()).isEqualByComparingTo("0.00");
    }

    @Test
    void blankCouponCodeIsRejectedBeforeRepositoryAccess() {
        assertThatThrownBy(() -> service.calculateDiscount(1L, "   ", new BigDecimal("1000.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Coupon code is required");
        verifyNoInteractions(couponRepository, usageRepository);
    }

    @Test
    void negativeSubtotalIsRejectedBeforeRepositoryAccess() {
        assertThatThrownBy(() -> service.calculateDiscount(1L, "SAVE20", new BigDecimal("-1.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Subtotal must be zero or greater");
        verifyNoInteractions(couponRepository, usageRepository);
    }

    @Test
    void reservationIncrementsUsageAndPersistsCouponUsage() {
        Coupon coupon = validCoupon(CouponType.FIXED, "100.00");
        when(couponRepository.findWithLockByCodeIgnoreCase("SAVE100")).thenReturn(Optional.of(coupon));
        when(usageRepository.existsByCouponIdAndUserId(10L, 1L)).thenReturn(false);

        DiscountResponse result = service.calculateAndReserve(1L, "SAVE100", new BigDecimal("1000.00"), user);

        assertThat(result.getDiscount()).isEqualByComparingTo("100.00");
        assertThat(coupon.getUsedCount()).isEqualTo(1);
        verify(usageRepository).save(any());
    }

    @Test
    void reservationAtUsageLimitIsRejected() {
        Coupon coupon = validCoupon(CouponType.FIXED, "100.00");
        coupon.setUsageLimit(2);
        coupon.setUsedCount(2);
        when(couponRepository.findWithLockByCodeIgnoreCase("LIMIT")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> service.calculateAndReserve(1L, "LIMIT", new BigDecimal("1000.00"), user))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("usage limit");
        verify(usageRepository, never()).save(any());
    }

    @Test
    void releaseReservationRemovesUsageAndDecrementsCount() {
        Coupon coupon = validCoupon(CouponType.FIXED, "100.00");
        coupon.setUsedCount(3);
        var usage = com.bookstore.entity.CouponUsage.builder().id(99L).coupon(coupon).user(user).build();
        when(couponRepository.findWithLockByCodeIgnoreCase("SAVE100")).thenReturn(Optional.of(coupon));
        when(usageRepository.findByCouponIdAndUserId(10L, 1L)).thenReturn(Optional.of(usage));

        service.releaseReservation(1L, "SAVE100");

        assertThat(coupon.getUsedCount()).isEqualTo(2);
        verify(usageRepository).delete(usage);
    }

    @Test
    void releaseReservationDoesNothingForMissingCoupon() {
        when(couponRepository.findWithLockByCodeIgnoreCase("MISSING")).thenReturn(Optional.empty());

        service.releaseReservation(1L, "MISSING");

        verifyNoInteractions(usageRepository);
    }

    private Coupon validCoupon(CouponType type, String value) {
        return Coupon.builder().id(10L).code("TEST").type(type).value(new BigDecimal(value))
                .minOrderAmount(new BigDecimal("500.00")).maxDiscountAmount(null)
                .usageLimit(10).usedCount(0).active(true)
                .startsAt(LocalDateTime.now().minusHours(1)).expiresAt(LocalDateTime.now().plusHours(1)).build();
    }
}
