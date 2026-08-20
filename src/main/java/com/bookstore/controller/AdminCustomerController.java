package com.bookstore.controller;

import com.bookstore.service.AdminCustomerService;
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
@RequestMapping("/admin/customers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCustomerController {
    private final AdminCustomerService customerService;

    @GetMapping
    public String customers(@RequestParam(required = false) String keyword,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        var customers = customerService.search(keyword,
                PageRequest.of(Math.max(page, 0), 20, Sort.by("createdAt").descending()));
        model.addAttribute("customers", customers);
        model.addAttribute("keyword", keyword);
        return "admin/customers";
    }
}
