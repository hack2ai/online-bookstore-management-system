package com.bookstore.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Adds a bounded correlation ID and safe request lifecycle logging to every request.
 *
 * <p>Client supplied IDs are accepted only when they match the safe format below;
 * otherwise a new UUID is generated. The value is placed in SLF4J MDC so every
 * log statement emitted during the request can be correlated without logging
 * authentication headers, cookies, request bodies, or query parameters.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Request-ID";
    public static final String MDC_KEY = "requestId";

    private static final int MAX_ID_LENGTH = 64;
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._:-]{1,64}");

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestId = sanitizeIncomingId(request.getHeader(HEADER_NAME));
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER_NAME, requestId);
        long startedAt = System.nanoTime();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            log.info("HTTP {} {} -> {} ({} ms)",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);
            MDC.remove(MDC_KEY);
        }
    }

    private String sanitizeIncomingId(String incomingId) {
        if (incomingId != null) {
            String candidate = incomingId.trim();
            if (candidate.length() <= MAX_ID_LENGTH && SAFE_ID.matcher(candidate).matches()) {
                return candidate;
            }
        }
        return UUID.randomUUID().toString();
    }
}
