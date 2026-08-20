package com.bookstore.service;

import com.bookstore.dto.response.DiscountResponse;
import com.bookstore.entity.User;
import java.math.BigDecimal;

public interface CouponService {
    DiscountResponse calculateDiscount(Long userId, String code, BigDecimal subtotal);
    DiscountResponse calculateAndReserve(Long userId, String code, BigDecimal subtotal, User user);
}
