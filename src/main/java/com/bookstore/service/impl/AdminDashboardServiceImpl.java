package com.bookstore.service.impl;

import com.bookstore.dto.response.AdminDashboardResponse;
import com.bookstore.entity.OrderStatus;
import com.bookstore.entity.Role;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CategoryRepository;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardServiceImpl implements AdminDashboardService {
    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    public AdminDashboardResponse getDashboard() {
        return AdminDashboardResponse.builder()
                .bookCount(bookRepository.count())
                .categoryCount(categoryRepository.count())
                .customerCount(userRepository.countByRole(Role.CUSTOMER))
                .orderCount(orderRepository.count())
                .pendingOrders(orderRepository.countByStatus(OrderStatus.PENDING))
                .lowStockBooks(bookRepository.countByStockLessThanEqual(5))
                .paidRevenue(BigDecimal.ZERO)
                .build();
    }
}
