package com.bookstore.service.impl;

import com.bookstore.dto.response.AdminCustomerDetailResponse;
import com.bookstore.dto.response.AdminCustomerResponse;
import com.bookstore.entity.Role;
import com.bookstore.entity.User;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.service.AdminCustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCustomerServiceImpl implements AdminCustomerService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Override
    public Page<AdminCustomerResponse> search(String keyword, Pageable pageable) {
        String normalized = normalizeKeyword(keyword);
        return userRepository.searchByRole(Role.CUSTOMER, normalized, pageable)
                .map(this::toResponse);
    }

    @Override
    public AdminCustomerDetailResponse getDetail(Long customerId, Pageable pageable) {
        validateCustomerId(customerId);

        User user = userRepository.findById(customerId)
                .filter(candidate -> candidate.getRole() == Role.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));

        return AdminCustomerDetailResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .createdAt(user.getCreatedAt())
                .orderCount(orderRepository.countByUserId(user.getId()))
                .totalSpent(safeTotalSpent(user.getId()))
                .build();
    }

    private AdminCustomerResponse toResponse(User user) {
        return AdminCustomerResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .createdAt(user.getCreatedAt())
                .orderCount(orderRepository.countByUserId(user.getId()))
                .totalSpent(safeTotalSpent(user.getId()))
                .build();
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? null : normalized;
    }

    private BigDecimal safeTotalSpent(Long userId) {
        BigDecimal total = orderRepository.sumPaidAmountByUserId(userId);
        return total == null ? BigDecimal.ZERO : total;
    }

    private void validateCustomerId(Long customerId) {
        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("Customer ID must be greater than zero");
        }
    }
}
