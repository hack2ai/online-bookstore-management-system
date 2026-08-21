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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCustomerServiceImpl implements AdminCustomerService {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Override
    public Page<AdminCustomerResponse> search(String keyword, Pageable pageable) {
        String normalized = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return userRepository.searchByRole(Role.CUSTOMER, normalized, pageable).map(this::toResponse);
    }

    @Override
    public AdminCustomerDetailResponse getDetail(Long customerId, Pageable pageable) {
        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
        if (user.getRole() != Role.CUSTOMER) {
            throw new ResourceNotFoundException("Customer", customerId);
        }
        return AdminCustomerDetailResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .createdAt(user.getCreatedAt())
                .orderCount(orderRepository.countByUserId(user.getId()))
                .totalSpent(orderRepository.sumPaidAmountByUserId(user.getId()))
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
                .totalSpent(orderRepository.sumPaidAmountByUserId(user.getId()))
                .build();
    }
}
