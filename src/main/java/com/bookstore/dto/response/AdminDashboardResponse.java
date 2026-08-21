package com.bookstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {
    private long bookCount;
    private long categoryCount;
    private long customerCount;
    private long orderCount;
    private long pendingOrders;
    private long lowStockBooks;
    private BigDecimal paidRevenue;
}
