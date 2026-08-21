package com.bookstore.service;

import com.bookstore.dto.response.AdminCustomerDetailResponse;
import com.bookstore.dto.response.AdminCustomerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminCustomerService {
    Page<AdminCustomerResponse> search(String keyword, Pageable pageable);
    AdminCustomerDetailResponse getDetail(Long customerId, Pageable pageable);
}
