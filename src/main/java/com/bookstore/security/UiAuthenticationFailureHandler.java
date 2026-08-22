package com.bookstore.security;

import com.bookstore.service.AuditService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class UiAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final AuditService auditService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        auditService.record(
                "LOGIN_FAILURE",
                null,
                "USER",
                null,
                "Web UI authentication failed"
        );

        response.sendRedirect("/auth/login?error=true");
    }
}
