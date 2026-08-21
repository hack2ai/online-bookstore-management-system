package com.bookstore.controller;

import com.bookstore.dto.request.CategoryRequest;
import com.bookstore.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryPageController {
    private final CategoryService categoryService;

    @GetMapping
    public String categories(Model model) {
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("categoryRequest", new CategoryRequest());
        return "admin/categories";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("categoryRequest") CategoryRequest request,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.getAll());
            return "admin/categories";
        }
        categoryService.create(request);
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid CategoryRequest request,
                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "redirect:/admin/categories";
        }
        categoryService.update(id, request);
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        categoryService.delete(id);
        return "redirect:/admin/categories";
    }
}
