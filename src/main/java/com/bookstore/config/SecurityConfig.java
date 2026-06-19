package com.bookstore.config;

import com.bookstore.security.CustomUserDetailsService;
import com.bookstore.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Two separate {@link SecurityFilterChain}s coexist in the same app:
 *
 * <ol>
 *   <li><b>API chain</b> (Order 1, matches /api/**): Stateless JWT.
 *       CSRF disabled (safe for stateless REST). Bearer token in each request.
 *       Returns 401/403 JSON, not login redirects.</li>
 *   <li><b>UI chain</b> (Order 2, matches everything else): Session-based.
 *       CSRF enabled (the default; required for Thymeleaf form POSTs).
 *       Redirects to /auth/login on 401. Standard Spring Security form login.</li>
 * </ol>
 *
 * {@code @EnableMethodSecurity} activates {@code @PreAuthorize} on service/
 * controller methods (e.g. {@code @PreAuthorize("hasRole('ADMIN')")}), which is
 * the preferred place to enforce role checks rather than hard-coding every URL
 * in {@code requestMatchers} rules here.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthFilter;

    // =========================================================================
    // Chain 1 — REST API (JWT, stateless)
    // =========================================================================

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public API endpoints — no token needed
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/books", "/api/books/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/categories", "/api/categories/**").permitAll()

                // Admin-only endpoints
                .requestMatchers(HttpMethod.POST,   "/api/books/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/books/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/books/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST,   "/api/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // Everything else under /api requires authentication
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // =========================================================================
    // Chain 2 — Thymeleaf UI (session-based)
    // =========================================================================

    @Bean
    @Order(2)
    public SecurityFilterChain uiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Static resources and public pages
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .requestMatchers("/", "/home", "/auth/login", "/auth/register").permitAll()
                .requestMatchers("/books", "/books/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                // Swagger UI (useful in dev — lock this down in production)
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                // Admin UI
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // Customer UI
                .requestMatchers("/cart/**", "/orders/**", "/profile/**").hasRole("CUSTOMER")

                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .defaultSuccessUrl("/home", true)
                .failureUrl("/auth/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/auth/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .authenticationProvider(authenticationProvider());

        return http.build();
    }

    // =========================================================================
    // Shared beans
    // =========================================================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt with strength 12 — a good balance between security and
        // registration latency (~300 ms on modern hardware). Bump to 13-14
        // if you have the compute budget.
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
