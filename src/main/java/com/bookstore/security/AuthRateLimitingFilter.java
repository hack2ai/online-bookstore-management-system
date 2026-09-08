package com.bookstore.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight application-level throttling for authentication endpoints.
 *
 * <p>This limiter deliberately uses the servlet remote address rather than trusting
 * arbitrary forwarding headers supplied by clients. It is intended as defense in
 * depth for a single application instance; a distributed deployment should place
 * the authoritative limit at the API gateway/load balancer as well.</p>
 */
@Component
public class AuthRateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_WINDOW = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final String RETRY_AFTER_SECONDS = "60";

    private final Map<String, RequestWindow> windows = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public AuthRateLimitingFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        return !"POST".equalsIgnoreCase(method)
                || !("/api/auth/login".equals(uri)
                || "/api/auth/register".equals(uri)
                || "/api/auth/refresh".equals(uri));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String key = request.getRemoteAddr() + '|' + request.getRequestURI();
        Instant now = Instant.now();
        RequestWindow window = windows.compute(key, (ignored, current) -> {
            if (current == null || Duration.between(current.startedAt(), now).compareTo(WINDOW) >= 0) {
                return new RequestWindow(now, 1);
            }
            return new RequestWindow(current.startedAt(), current.count() + 1);
        });

        cleanupExpiredWindows(now);

        if (window.count() > MAX_REQUESTS_PER_WINDOW) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", RETRY_AFTER_SECONDS);
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "success", false,
                    "message", "Too many authentication requests. Please try again later."
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void cleanupExpiredWindows(Instant now) {
        windows.entrySet().removeIf(entry ->
                Duration.between(entry.getValue().startedAt(), now).compareTo(WINDOW) >= 0);
    }

    private record RequestWindow(Instant startedAt, int count) {
    }
}
