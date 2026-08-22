package com.bookstore.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestMetricsServiceTest {

    @Test
    void recordsTotalsErrorsAverageAndTopEndpoints() {
        RequestMetricsService service = new RequestMetricsService();

        service.record("GET", "/books", 200, 10);
        service.record("GET", "/books", 200, 20);
        service.record("POST", "/orders", 201, 30);
        service.record("GET", "/missing", 404, 40);
        service.record("GET", "/broken", 500, 50);

        RequestMetricsService.MetricsSnapshot snapshot = service.snapshot();

        assertEquals(5, snapshot.getTotalRequests());
        assertEquals(3, snapshot.getSuccessfulResponses());
        assertEquals(1, snapshot.getClientErrors());
        assertEquals(1, snapshot.getServerErrors());
        assertEquals(30.0, snapshot.getAverageDurationMs());
        assertEquals("GET", snapshot.getTopEndpoints().get(0).getMethod());
        assertEquals("/books", snapshot.getTopEndpoints().get(0).getPath());
        assertEquals(2, snapshot.getTopEndpoints().get(0).getRequestCount());
    }
}
