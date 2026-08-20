package com.bookstore.repository;

import com.bookstore.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {
    boolean existsByCouponIdAndUserId(Long couponId, Long userId);
}
