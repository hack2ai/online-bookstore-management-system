package com.bookstore.controller;

import com.bookstore.entity.OrderStatus;
import com.bookstore.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderPageController {
    private final OrderService orderService;

    @GetMapping
    public String orders(@RequestParam(required = false) OrderStatus status,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        var orders = orderService.getAllOrders(
                status,
                PageRequest.of(Math.max(page, 0), 20, Sort.by("orderDate").descending())
        );
        model.addAttribute("orders", orders);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", OrderStatus.values());
        return "admin/orders";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam OrderStatus status) {
        orderService.updateStatus(id, status);
        return "redirect:/admin/orders";
    }
}
