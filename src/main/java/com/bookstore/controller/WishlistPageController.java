package com.bookstore.controller;

import com.bookstore.security.CustomUserDetails;
import com.bookstore.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/wishlist")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class WishlistPageController {

    private final WishlistService wishlistService;

    @GetMapping
    public String wishlist(Authentication authentication, Model model) {
        model.addAttribute("items", wishlistService.getWishlist(userId(authentication)));
        return "wishlist";
    }

    @PostMapping("/{bookId}")
    public String add(Authentication authentication, @PathVariable Long bookId) {
        wishlistService.add(userId(authentication), bookId);
        return "redirect:/wishlist";
    }

    @PostMapping("/{bookId}/remove")
    public String remove(Authentication authentication, @PathVariable Long bookId) {
        wishlistService.remove(userId(authentication), bookId);
        return "redirect:/wishlist";
    }

    private Long userId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
    }
}
