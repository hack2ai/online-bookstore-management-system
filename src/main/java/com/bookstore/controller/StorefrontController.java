package com.bookstore.controller;

import com.bookstore.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class StorefrontController {

    private final BookService bookService;

    @GetMapping({"/", "/books"})
    public String home(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        Page<?> books = bookService.search(keyword, categoryId, PageRequest.of(
                Math.max(page, 0), 12, Sort.by(Sort.Direction.DESC, "createdAt")));
        model.addAttribute("books", books);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        return "index";
    }

    @GetMapping("/books/{id}")
    public String bookDetails(@PathVariable Long id, Model model) {
        model.addAttribute("book", bookService.getById(id));
        return "book-details";
    }
}
