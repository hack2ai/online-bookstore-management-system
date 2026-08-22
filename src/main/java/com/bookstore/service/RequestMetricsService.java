package com.bookstore.service;

import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RequestMetricsService {

    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong successfulResponses = new AtomicLong();
    private final AtomicLong clientErrors = new AtomicLong();
    private final AtomicLong serverErrors = new AtomicLong();
    private final AtomicLong totalDurationMs = new AtomicLong();
    private final ConcurrentHashMap<String, EndpointStats> endpointStats = new ConcurrentHashMap<>();

    public void record(String method, String path, int status, long durationMs) {
        totalRequests.incrementAndGet();
        if (status >= 200 && status < 400) {
            successfulResponses.incrementAndGet();
        } else if (status >= 400 && status < 500) {
            clientErrors.incrementAndGet();
        } else if (status >= 500) {
            serverErrors.incrementAndGet();
        }
        totalDurationMs.addAndGet(Math.max(0, durationMs));
        endpointStats.computeIfAbsent(method + " " + path, ignored -> new EndpointStats(method, path))
                .record(status, durationMs);
    }

    public MetricsSnapshot snapshot() {
        long total = totalRequests.get();
        double average = total == 0 ? 0.0 : (double) totalDurationMs.get() / total;
        List<EndpointSnapshot> topEndpoints = new ArrayList<>();
        endpointStats.values().forEach(stats -> topEndpoints.add(stats.snapshot()));
        topEndpoints.sort(Comparator.comparingLong(EndpointSnapshot::getRequestCount).reversed());
        if (topEndpoints.size() > 8) {
            topEndpoints = new ArrayList<>(topEndpoints.subList(0, 8));
        }
        return new MetricsSnapshot(total, successfulResponses.get(), clientErrors.get(), serverErrors.get(), average, topEndpoints);
    }

    @Getter
    public static final class MetricsSnapshot {
        private final long totalRequests;
        private final long successfulResponses;
        private final long clientErrors;
        private final long serverErrors;
        private final double averageDurationMs;
        private final List<EndpointSnapshot> topEndpoints;

        public MetricsSnapshot(long totalRequests, long successfulResponses, long clientErrors,
                               long serverErrors, double averageDurationMs, List<EndpointSnapshot> topEndpoints) {
            this.totalRequests = totalRequests;
            this.successfulResponses = successfulResponses;
            this.clientErrors = clientErrors;
            this.serverErrors = serverErrors;
            this.averageDurationMs = averageDurationMs;
            this.topEndpoints = List.copyOf(topEndpoints);
        }
    }

    @Getter
    public static final class EndpointSnapshot {
        private final String method;
        private final String path;
        private final long requestCount;
        private final long errorCount;
        private final double averageDurationMs;

        public EndpointSnapshot(String method, String path, long requestCount, long errorCount, double averageDurationMs) {
            this.method = method;
            this.path = path;
            this.requestCount = requestCount;
            this.errorCount = errorCount;
            this.averageDurationMs = averageDurationMs;
        }
    }

    private static final class EndpointStats {
        private final String method;
        private final String path;
        private final AtomicLong requestCount = new AtomicLong();
        private final AtomicLong errorCount = new AtomicLong();
        private final AtomicLong totalDurationMs = new AtomicLong();

        private EndpointStats(String method, String path) {
            this.method = method;
            this.path = path;
        }

        private void record(int status, long durationMs) {
            requestCount.incrementAndGet();
            if (status >= 400) {
                errorCount.incrementAndGet();
            }
            totalDurationMs.addAndGet(Math.max(0, durationMs));
        }

        private EndpointSnapshot snapshot() {
            long count = requestCount.get();
            double average = count == 0 ? 0.0 : (double) totalDurationMs.get() / count;
            return new EndpointSnapshot(method, path, count, errorCount.get(), average);
        }
    }
}
