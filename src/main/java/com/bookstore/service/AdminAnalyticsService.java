package com.bookstore.service;

import com.bookstore.dto.response.AdminAnalyticsResponse;

import java.time.LocalDate;

public interface AdminAnalyticsService {
    AdminAnalyticsResponse getAnalytics();
    AdminAnalyticsResponse getAnalytics(LocalDate from, LocalDate to);
}
