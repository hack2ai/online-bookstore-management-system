package com.bookstore.controller;

import com.bookstore.dto.response.AdminAnalyticsResponse;
import com.bookstore.service.AdminAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/api/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Analytics", description = "Administrative sales and inventory analytics")
public class AdminAnalyticsController {

    private final AdminAnalyticsService analyticsService;

    @GetMapping
    @Operation(summary = "Get admin analytics", description = "Returns revenue, order, inventory, and best-selling-book metrics. Date filters are optional and inclusive.")
    @SecurityRequirement(name = "bearerAuth")
    public AdminAnalyticsResponse analytics(
            @Parameter(description = "Inclusive start date in ISO format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @Parameter(description = "Inclusive end date in ISO format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to) {
        return analyticsService.getAnalytics(from, to);
    }
}
