package com.bookstore.dto.response;

import com.bookstore.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long orderId;
    private String razorpayOrderId;
    private String transactionId;
    private PaymentStatus status;
}
