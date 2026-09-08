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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Resolves an optional Bearer JWT and places an authenticated principal in the
 * Spring Security context when the token and corresponding user are valid.
 * Invalid JWTs are deliberately left to the security rules so public routes
 * can continue normally while protected routes receive the appropriate denial.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String jwt = extractJwtFromRequest(request);

        if (jwt == null || !jwtUtil.validateToken(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                final String email = jwtUtil.extractUsername(jwt);

                if (StringUtils.hasText(email)) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    if (jwtUtil.isTokenValid(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        log.debug("JWT authentication accepted for request {}", request.getRequestURI());
                    }
                }
            } catch (UsernameNotFoundException ex) {
                // A valid token for a deleted/unknown account must not become a
                // server error. Leave the request unauthenticated.
                log.debug("JWT subject does not map to an existing user");
            } catch (RuntimeException ex) {
                // Never turn token parsing/user lookup issues into an unexpected
                // 500 response from the authentication filter.
                log.debug("JWT authentication could not be established: {}", ex.getClass().getSimpleName());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Returns the trimmed token from a valid Bearer Authorization header.
     * Missing, malformed, or blank headers are treated as unauthenticated.
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return StringUtils.hasText(token) ? token : null;
    }
}
