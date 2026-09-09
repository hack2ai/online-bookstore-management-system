package com.bookstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentVerifyRequest {

    @NotBlank
    @Size(max = 100)
    private String razorpayOrderId;

    @NotBlank
    @Size(max = 100)
    private String razorpayPaymentId;

    @NotBlank
    @Size(max = 100)
    private String razorpaySignature;
}
