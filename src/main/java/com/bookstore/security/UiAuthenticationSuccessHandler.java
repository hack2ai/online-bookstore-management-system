package com.bookstore.security;

import com.bookstore.service.AuditService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class UiAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuditService auditService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        Long userId = null;
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            userId = userDetails.getUser().getId();
        }

        auditService.record(
                "LOGIN_SUCCESS",
                userId,
                "USER",
                userId,
                "User authenticated successfully via web UI"
        );

        request.setAttribute(LoginRateLimitFilter.LOGIN_SUCCESS_ATTRIBUTE, Boolean.TRUE);
        response.sendRedirect("/");
    }
}
