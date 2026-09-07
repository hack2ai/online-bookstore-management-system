package com.bookstore.service.impl;

import com.bookstore.dto.response.AdminAnalyticsResponse;
import com.bookstore.dto.response.BestSellingBookResponse;
import com.bookstore.entity.Book;
import com.bookstore.entity.OrderStatus;
import com.bookstore.entity.PaymentStatus;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.OrderItemRepository;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.PaymentRepository;
import com.bookstore.service.AdminAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private static final int BEST_SELLING_LIMIT = 5;
    private static final int LOW_STOCK_THRESHOLD = 5;

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    public AdminAnalyticsResponse getAnalytics() {
        return build(null, null);
    }

    @Override
    public AdminAnalyticsResponse getAnalytics(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return build(null, null);
        }

        LocalDate safeFrom = from == null ? to : from;
        LocalDate safeTo = to == null ? from : to;
        if (safeFrom.isAfter(safeTo)) {
            LocalDate swap = safeFrom;
            safeFrom = safeTo;
            safeTo = swap;
        }

        return build(safeFrom.atStartOfDay(), safeTo.plusDays(1).atStartOfDay());
    }

    private AdminAnalyticsResponse build(LocalDateTime from, LocalDateTime to) {
        BigDecimal revenue;
        long paidOrders;
        long pendingOrders;
        long cancelledOrders;

        if (from == null) {
            revenue = safeAmount(paymentRepository.sumOrderAmountsByStatus(PaymentStatus.SUCCESS));
            paidOrders = paymentRepository.countByPaymentStatus(PaymentStatus.SUCCESS);
            pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
            cancelledOrders = orderRepository.countByStatus(OrderStatus.CANCELLED);
        } else {
            revenue = safeAmount(orderRepository.sumPaidAmountBetween(from, to));
            paidOrders = orderRepository.countPaidOrdersBetween(from, to);
            pendingOrders = orderRepository.countByStatusAndOrderDateGreaterThanEqualAndOrderDateLessThan(
                    OrderStatus.PENDING, from, to);
            cancelledOrders = orderRepository.countByStatusAndOrderDateGreaterThanEqualAndOrderDateLessThan(
                    OrderStatus.CANCELLED, from, to);
        }

        BigDecimal average = paidOrders == 0
                ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(paidOrders), 2, RoundingMode.HALF_UP);

        List<BestSellingBookResponse> bestSelling = loadBestSellingBooks();

        return AdminAnalyticsResponse.builder()
                .paidRevenue(revenue)
                .paidOrders(paidOrders)
                .averageOrderValue(average)
                .pendingOrders(pendingOrders)
                .cancelledOrders(cancelledOrders)
                .lowStockBooks(bookRepository.countByStockLessThanEqual(LOW_STOCK_THRESHOLD))
                .bestSellingBooks(bestSelling)
                .build();
    }

    private List<BestSellingBookResponse> loadBestSellingBooks() {
        List<Object[]> rows = orderItemRepository.findBestSellingBooks(PageRequest.of(0, BEST_SELLING_LIMIT));
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        return rows.stream()
                .filter(row -> row != null && row.length >= 2 && row[0] instanceof Book && row[1] instanceof Number)
                .map(row -> {
                    Book book = (Book) row[0];
                    Number sold = (Number) row[1];
                    return BestSellingBookResponse.builder()
                            .bookId(book.getId())
                            .title(book.getTitle())
                            .author(book.getAuthor())
                            .unitsSold(sold.longValue())
                            .build();
                })
                .toList();
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
