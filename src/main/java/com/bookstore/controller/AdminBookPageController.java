package com.bookstore.controller;

import com.bookstore.dto.request.BookRequest;
import com.bookstore.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/books")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookPageController {
    private final BookService bookService;

    @GetMapping
    public String books(@RequestParam(required = false) String keyword,
                        @RequestParam(defaultValue = "0") int page, Model model) {
        Page<?> books = bookService.search(keyword, null, PageRequest.of(Math.max(page, 0), 20, Sort.by("title").ascending()));
        model.addAttribute("books", books);
        model.addAttribute("keyword", keyword);
        return "admin/books";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("bookId", null);
        return "admin/book-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute BookRequest request) {
        bookService.create(request);
        return "redirect:/admin/books";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("bookId", id);
        model.addAttribute("book", bookService.getById(id));
        return "admin/book-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute BookRequest request) {
        bookService.update(id, request);
        return "redirect:/admin/books";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        bookService.delete(id);
        return "redirect:/admin/books";
    }
}
