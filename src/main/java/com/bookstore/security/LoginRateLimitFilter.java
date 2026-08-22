package com.bookstore.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    static final String LOGIN_FAILURE_ATTRIBUTE = LoginRateLimitFilter.class.getName() + ".failure";
    static final String LOGIN_SUCCESS_ATTRIBUTE = LoginRateLimitFilter.class.getName() + ".success";

    private static final String UI_LOGIN = "/auth/login";
    private static final String API_LOGIN = "/api/auth/login";

    private final int maxFailures;
    private final Duration window;
    private final ConcurrentHashMap<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    public LoginRateLimitFilter(
            @Value("${app.security.login-rate-limit.max-failures:5}") int maxFailures,
            @Value("${app.security.login-rate-limit.window-ms:300000}") long windowMs) {
        if (maxFailures < 1) {
            throw new IllegalArgumentException("max-failures must be positive");
        }
        if (windowMs < 1) {
            throw new IllegalArgumentException("window-ms must be positive");
        }
        this.maxFailures = maxFailures;
        this.window = Duration.ofMillis(windowMs);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!isLoginRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = clientKey(request);
        if (isBlocked(key)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", Long.toString(Math.max(1, window.toSeconds())));
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many login attempts. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);

        if (Boolean.TRUE.equals(request.getAttribute(LOGIN_SUCCESS_ATTRIBUTE))
                || (response.getStatus() < HttpStatus.BAD_REQUEST.value()
                && !Boolean.TRUE.equals(request.getAttribute(LOGIN_FAILURE_ATTRIBUTE)))) {
            attempts.remove(key);
            return;
        }

        if (Boolean.TRUE.equals(request.getAttribute(LOGIN_FAILURE_ATTRIBUTE))
                || response.getStatus() >= HttpStatus.BAD_REQUEST.value()) {
            recordFailure(key);
        }
    }

    boolean isBlocked(String key) {
        AttemptWindow current = attempts.get(key);
        if (current == null) {
            return false;
        }
        if (current.isExpired(window)) {
            attempts.remove(key, current);
            return false;
        }
        return current.failures.get() >= maxFailures;
    }

    void recordFailure(String key) {
        Instant now = Instant.now();
        attempts.compute(key, (ignored, current) -> {
            if (current == null || current.isExpired(window)) {
                return new AttemptWindow(now);
            }
            current.failures.incrementAndGet();
            return current;
        });
    }

    int failuresFor(String key) {
        AttemptWindow current = attempts.get(key);
        if (current == null || current.isExpired(window)) {
            return 0;
        }
        return current.failures.get();
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && (UI_LOGIN.equals(request.getServletPath()) || API_LOGIN.equals(request.getServletPath()));
    }

    private String clientKey(HttpServletRequest request) {
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private static final class AttemptWindow {
        private final Instant startedAt;
        private final AtomicInteger failures = new AtomicInteger(1);

        private AttemptWindow(Instant startedAt) {
            this.startedAt = startedAt;
        }

        private boolean isExpired(Duration window) {
            return Instant.now().isAfter(startedAt.plus(window));
        }
    }
}
