package com.bookstore.controller;

import com.bookstore.dto.response.AdminAnalyticsResponse;
import com.bookstore.service.AdminAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {
    private final AdminAnalyticsService analyticsService;

    @GetMapping
    public AdminAnalyticsResponse analytics() {
        return analyticsService.getAnalytics();
    }
}
