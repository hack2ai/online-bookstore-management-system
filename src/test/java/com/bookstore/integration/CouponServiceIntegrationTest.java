package com.bookstore.integration;

import com.bookstore.dto.response.DiscountResponse;
import com.bookstore.entity.Coupon;
import com.bookstore.entity.CouponType;
import com.bookstore.entity.Role;
import com.bookstore.entity.User;
import com.bookstore.repository.CouponRepository;
import com.bookstore.repository.CouponUsageRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.service.impl.CouponServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CouponServiceIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired CouponRepository couponRepository;
    @Autowired CouponUsageRepository usageRepository;
    @Autowired CouponServiceImpl couponService;

    private User user;
    private Coupon coupon;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .name("Coupon User")
                .email("coupon@example.com")
                .password("hashed-password")
                .role(Role.CUSTOMER)
                .build());

        LocalDateTime now = LocalDateTime.now();
        coupon = couponRepository.save(Coupon.builder()
                .code("SAVE10")
                .type(CouponType.PERCENTAGE)
                .value(new BigDecimal("10.00"))
                .minOrderAmount(new BigDecimal("100.00"))
                .maxDiscountAmount(new BigDecimal("150.00"))
                .usageLimit(2)
                .usedCount(0)
                .startsAt(now.minusMinutes(5))
                .expiresAt(now.plusHours(1))
                .active(true)
                .build());
    }

    @Test
    void calculateAndReservePersistsUsageAndIncrementsCount() {
        DiscountResponse response = couponService.calculateAndReserve(
                user.getId(), " save10 ", new BigDecimal("500.00"), user);

        assertThat(response.getCode()).isEqualTo("SAVE10");
        assertThat(response.getDiscount()).isEqualByComparingTo("50.00");
        assertThat(response.getDiscountedSubtotal()).isEqualByComparingTo("450.00");

        Coupon persisted = couponRepository.findById(coupon.getId()).orElseThrow();
        assertThat(persisted.getUsedCount()).isEqualTo(1);
        assertThat(usageRepository.existsByCouponIdAndUserId(coupon.getId(), user.getId())).isTrue();
    }

    @Test
    void secondReservationBySameUserIsRejected() {
        couponService.calculateAndReserve(user.getId(), "SAVE10", new BigDecimal("500.00"), user);

        assertThatThrownBy(() -> couponService.calculateAndReserve(
                user.getId(), "SAVE10", new BigDecimal("500.00"), user))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already used");
    }

    @Test
    void releaseReservationRemovesUsageAndDecrementsCount() {
        couponService.calculateAndReserve(user.getId(), "SAVE10", new BigDecimal("500.00"), user);

        couponService.releaseReservation(user.getId(), "SAVE10");

        Coupon persisted = couponRepository.findById(coupon.getId()).orElseThrow();
        assertThat(persisted.getUsedCount()).isZero();
        assertThat(usageRepository.existsByCouponIdAndUserId(coupon.getId(), user.getId())).isFalse();
    }

    @Test
    void inactiveCouponIsRejected() {
        coupon.setActive(false);
        couponRepository.save(coupon);

        assertThatThrownBy(() -> couponService.calculateDiscount(
                user.getId(), "SAVE10", new BigDecimal("500.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not currently active");
    }
}
