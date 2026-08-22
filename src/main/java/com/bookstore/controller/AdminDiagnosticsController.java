package com.bookstore.controller;

import com.bookstore.dto.response.AdminDashboardResponse;
import com.bookstore.repository.AuditEventRepository;
import com.bookstore.service.AdminDashboardService;
import com.bookstore.service.RequestMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/diagnostics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDiagnosticsController {

    private final HealthEndpoint healthEndpoint;
    private final JdbcTemplate jdbcTemplate;
    private final AdminDashboardService dashboardService;
    private final AuditEventRepository auditEventRepository;
    private final Environment environment;
    private final RequestMetricsService requestMetricsService;

    @GetMapping
    public String diagnostics(Model model) {
        AdminDashboardResponse dashboard = dashboardService.getDashboard();
        RequestMetricsService.MetricsSnapshot metrics = requestMetricsService.snapshot();

        model.addAttribute("healthStatus", healthEndpoint.health().getStatus().getCode());
        model.addAttribute("databaseStatus", databaseStatus());
        model.addAttribute("activeProfile", String.join(", ", environment.getActiveProfiles()));
        model.addAttribute("applicationName", environment.getProperty("spring.application.name", "bookstore"));
        model.addAttribute("javaVersion", System.getProperty("java.version"));
        model.addAttribute("bookCount", dashboard.getBookCount());
        model.addAttribute("orderCount", dashboard.getOrderCount());
        model.addAttribute("customerCount", dashboard.getCustomerCount());
        model.addAttribute("auditEventCount", auditEventRepository.count());
        model.addAttribute("metrics", metrics);

        return "admin/diagnostics";
    }

    private String databaseStatus() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Integer.valueOf(1).equals(result) ? "UP" : "DOWN";
        } catch (Exception exception) {
            return "DOWN";
        }
    }
}
