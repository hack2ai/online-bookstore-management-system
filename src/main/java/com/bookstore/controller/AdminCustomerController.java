package com.bookstore.controller;

import com.bookstore.service.AdminCustomerService;
import com.bookstore.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/customers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCustomerController {

    private static final int PAGE_SIZE = 20;
    private static final int MAX_PAGE = 10_000;
    private static final int MAX_KEYWORD_LENGTH = 100;

    private final AdminCustomerService customerService;
    private final OrderService orderService;

    @GetMapping
    public String customers(@RequestParam(required = false) String keyword,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        String normalizedKeyword = normalizeKeyword(keyword);
        int safePage = Math.min(Math.max(page, 0), MAX_PAGE);

        var customers = customerService.search(normalizedKeyword,
                PageRequest.of(safePage, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")));

        model.addAttribute("customers", customers);
        model.addAttribute("keyword", normalizedKeyword);
        model.addAttribute("currentPage", safePage);
        return "admin/customers";
    }

    @GetMapping("/{id}")
    public String customer(@PathVariable Long id, Model model) {
        validateCustomerId(id);

        var customer = customerService.getDetail(id, PageRequest.of(0, 1));
        var orders = orderService.getMyOrders(id,
                PageRequest.of(0, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "orderDate")));

        model.addAttribute("customer", customer);
        model.addAttribute("orders", orders);
        return "admin/customer-detail";
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.substring(0, Math.min(normalized.length(), MAX_KEYWORD_LENGTH));
    }

    private void validateCustomerId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Customer ID must be greater than zero");
        }
    }
}
