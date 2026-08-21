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
    private final BookService bookService;

    @GetMapping
    public String inventory(@RequestParam(defaultValue = "5") int threshold,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        var books = bookService.search(null, null,
                PageRequest.of(Math.max(page, 0), 20, Sort.by("stock").ascending()));
        model.addAttribute("books", books);
        model.addAttribute("threshold", Math.max(threshold, 0));
        return "admin/inventory";
    }
}
