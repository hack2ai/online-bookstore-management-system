package com.bookstore.security;

import com.bookstore.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UiLogoutAuditHandler implements LogoutHandler {

    private final AuditService auditService;

    @Override
    public void logout(HttpServletRequest request,
                       HttpServletResponse response,
                       Authentication authentication) {
        if (authentication == null) {
            return;
        }

        Long userId = null;
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            userId = userDetails.getUser().getId();
        }

        auditService.record(
                "LOGOUT",
                userId,
                "USER",
                userId,
                "User logged out via web UI"
        );
    }
}
