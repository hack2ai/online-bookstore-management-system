package com.bookstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CouponApplyRequest {
    @NotBlank
    @Size(max = 40)
    private String code;
}
