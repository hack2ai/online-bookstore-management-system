package com.bookstore.service;

import com.bookstore.dto.response.DiscountResponse;
import java.math.BigDecimal;

public interface CouponService {
    DiscountResponse calculateDiscount(Long userId, String code, BigDecimal subtotal);
}
