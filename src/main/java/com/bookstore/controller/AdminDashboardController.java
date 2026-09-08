package com.bookstore.controller;

import com.bookstore.dto.response.AdminDashboardResponse;
import com.bookstore.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Dashboard", description = "Administrative dashboard and overview metrics")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Open admin dashboard")
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
    @Operation(summary = "Get admin dashboard metrics")
    public ResponseEntity<AdminDashboardResponse> dashboardApi() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(dashboardService.getDashboard());
    }
}
