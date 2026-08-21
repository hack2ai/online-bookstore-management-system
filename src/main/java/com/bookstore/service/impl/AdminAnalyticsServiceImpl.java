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
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {
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
        if (from == null && to == null) return build(null, null);
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
            revenue = paymentRepository.sumOrderAmountsByStatus(PaymentStatus.SUCCESS);
            paidOrders = paymentRepository.countByPaymentStatus(PaymentStatus.SUCCESS);
            pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
            cancelledOrders = orderRepository.countByStatus(OrderStatus.CANCELLED);
        } else {
            revenue = orderRepository.sumPaidAmountBetween(from, to);
            paidOrders = orderRepository.countPaidOrdersBetween(from, to);
            pendingOrders = orderRepository.countByStatusAndOrderDateGreaterThanEqualAndOrderDateLessThan(OrderStatus.PENDING, from, to);
            cancelledOrders = orderRepository.countByStatusAndOrderDateGreaterThanEqualAndOrderDateLessThan(OrderStatus.CANCELLED, from, to);
        }

        BigDecimal average = paidOrders == 0
                ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(paidOrders), 2, RoundingMode.HALF_UP);

        List<BestSellingBookResponse> bestSelling = orderItemRepository.findBestSellingBooks(PageRequest.of(0, 5))
                .stream()
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

        return AdminAnalyticsResponse.builder()
                .paidRevenue(revenue)
                .paidOrders(paidOrders)
                .averageOrderValue(average)
                .pendingOrders(pendingOrders)
                .cancelledOrders(cancelledOrders)
                .lowStockBooks(bookRepository.countByStockLessThanEqual(5))
                .bestSellingBooks(bestSelling)
                .build();
    }
}
