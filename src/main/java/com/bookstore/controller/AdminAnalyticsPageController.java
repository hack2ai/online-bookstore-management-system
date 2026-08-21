package com.bookstore.controller;

import com.bookstore.service.AdminAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsPageController {
    private final AdminAnalyticsService analyticsService;

    @GetMapping
    public String analytics(Model model) {
        model.addAttribute("analytics", analyticsService.getAnalytics());
        return "admin/analytics";
    }
}
