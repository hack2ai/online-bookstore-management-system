package com.bookstore.controller;

import com.bookstore.service.AdminAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditPageController {

    private final AdminAuditService auditService;

    @GetMapping
    public String audit(@RequestParam(required = false) String eventType,
                        @RequestParam(required = false) Long userId,
                        @RequestParam(required = false) String requestId,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        var events = auditService.search(
                eventType,
                userId,
                requestId,
                PageRequest.of(Math.max(page, 0), 25, Sort.by("createdAt").descending())
        );
        model.addAttribute("events", events);
        model.addAttribute("eventType", eventType);
        model.addAttribute("userId", userId);
        model.addAttribute("requestId", requestId);
        return "admin/audit";
    }
}
