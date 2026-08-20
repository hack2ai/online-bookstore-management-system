package com.bookstore.controller;

import com.bookstore.dto.response.AdminDashboardResponse;
import com.bookstore.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {
    private final AdminDashboardService dashboardService;

    @GetMapping
    public String dashboard(Model model) {
        AdminDashboardResponse dashboard = dashboardService.getDashboard();
        model.addAttribute("bookCount", dashboard.getBookCount());
        model.addAttribute("categoryCount", dashboard.getCategoryCount());
        model.addAttribute("customerCount", dashboard.getCustomerCount());
        model.addAttribute("orderCount", dashboard.getOrderCount());
        model.addAttribute("pendingOrders", dashboard.getPendingOrders());
        model.addAttribute("lowStockBooks", dashboard.getLowStockBooks());
        model.addAttribute("paidRevenue", dashboard.getPaidRevenue());
        return "admin/dashboard";
    }

    @GetMapping("/api/dashboard")
    @ResponseBody
    public AdminDashboardResponse dashboardApi() {
        return dashboardService.getDashboard();
    }
}
