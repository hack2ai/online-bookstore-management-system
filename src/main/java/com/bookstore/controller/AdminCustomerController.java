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
    private final AdminCustomerService customerService;
    private final OrderService orderService;

    @GetMapping
    public String customers(@RequestParam(required = false) String keyword,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        var customers = customerService.search(keyword,
                PageRequest.of(Math.max(page, 0), 20, Sort.by("createdAt").descending()));
        model.addAttribute("customers", customers);
        model.addAttribute("keyword", keyword);
        return "admin/customers";
    }

    @GetMapping("/{id}")
    public String customer(@PathVariable Long id, Model model) {
        var customer = customerService.getDetail(id, PageRequest.of(0, 1));
        var orders = orderService.getMyOrders(id,
                PageRequest.of(0, 20, Sort.by("orderDate").descending()));
        model.addAttribute("customer", customer);
        model.addAttribute("orders", orders);
        return "admin/customer-detail";
    }
}
