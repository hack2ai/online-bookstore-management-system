package com.bookstore.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DiscountResponse {
    private String code;
    private BigDecimal discount;
    private BigDecimal originalSubtotal;
    private BigDecimal discountedSubtotal;
}
