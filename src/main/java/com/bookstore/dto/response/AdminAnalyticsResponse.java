package com.bookstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAnalyticsResponse {
    private BigDecimal paidRevenue;
    private long paidOrders;
    private BigDecimal averageOrderValue;
    private long pendingOrders;
    private long cancelledOrders;
    private long lowStockBooks;
    private List<BestSellingBookResponse> bestSellingBooks;
}
