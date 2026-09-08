package com.bookstore.controller;

import com.bookstore.service.BookService;
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
@RequestMapping("/admin/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminInventoryController {

    private static final int PAGE_SIZE = 20;
    private static final int MAX_PAGE = 10_000;
    private static final int MIN_THRESHOLD = 0;
    private static final int MAX_THRESHOLD = 100_000;

    private final BookService bookService;

    @GetMapping
    public String inventory(@RequestParam(defaultValue = "5") int threshold,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        int safeThreshold = Math.min(Math.max(threshold, MIN_THRESHOLD), MAX_THRESHOLD);
        int safePage = Math.min(Math.max(page, 0), MAX_PAGE);

        var books = bookService.search(null, null,
                PageRequest.of(safePage, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "stock")));

        model.addAttribute("books", books);
        model.addAttribute("threshold", safeThreshold);
        model.addAttribute("currentPage", safePage);
        model.addAttribute("pageSize", PAGE_SIZE);
        return "admin/inventory";
    }
}
