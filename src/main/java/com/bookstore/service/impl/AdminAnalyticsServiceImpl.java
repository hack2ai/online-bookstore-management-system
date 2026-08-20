package com.bookstore.service.impl;

import com.bookstore.dto.response.AdminAnalyticsResponse;
import com.bookstore.entity.PaymentStatus;
import com.bookstore.repository.PaymentRepository;
import com.bookstore.service.AdminAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {
    private final PaymentRepository paymentRepository;

    @Override
    public AdminAnalyticsResponse getAnalytics() {
        BigDecimal revenue = paymentRepository.sumOrderAmountsByStatus(PaymentStatus.SUCCESS);
        long paidOrders = paymentRepository.countByPaymentStatus(PaymentStatus.SUCCESS);
        BigDecimal average = paidOrders == 0
                ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(paidOrders), 2, RoundingMode.HALF_UP);
        return AdminAnalyticsResponse.builder()
                .paidRevenue(revenue)
                .paidOrders(paidOrders)
                .averageOrderValue(average)
                .build();
    }
}
