package com.bookstore.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs once per request (extends {@link OncePerRequestFilter}).
 *
 * Logic:
 *  1. Extract "Authorization: Bearer <token>" header.
 *  2. If absent or malformed — do nothing; let the request proceed. Spring
 *     Security will block it at the access-control level if the endpoint
 *     requires authentication. We never short-circuit here because public
 *     endpoints (register, login, GET /api/books) must still pass through.
 *  3. Validate the token via {@link JwtUtil#validateToken}.
 *  4. Load the user from the DB (needed to get full authorities).
 *  5. Set the {@link org.springframework.security.core.Authentication} into
 *     {@link SecurityContextHolder} so downstream filters and controllers
 *     see an authenticated principal.
 *
 * This filter is added BEFORE {@code UsernamePasswordAuthenticationFilter}
 * in {@link com.bookstore.config.SecurityConfig}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String jwt = extractJwtFromRequest(request);

        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtUtil.validateToken(jwt)) {
            // validateToken already logged the specific failure reason
            filterChain.doFilter(request, response);
            return;
        }

        final String email = jwtUtil.extractUsername(jwt);

        // Only set authentication if not already set (avoids redundant DB hits
        // when multiple filters/interceptors touch the same request)
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (jwtUtil.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Authenticated user '{}' via JWT for request: {}", email, request.getRequestURI());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Pulls the raw token string out of the Authorization header.
     * Returns null (not throwing) for missing/malformed headers so the
     * filter can silently pass unauthenticated requests through to public
     * endpoints.
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
