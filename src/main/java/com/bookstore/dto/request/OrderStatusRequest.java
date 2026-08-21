package com.bookstore.dto.request;

import com.bookstore.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderStatusRequest {
    @NotNull(message = "Status is required")
    private OrderStatus status;
}
