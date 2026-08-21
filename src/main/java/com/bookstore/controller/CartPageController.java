package com.bookstore.controller;

import com.bookstore.dto.request.CartItemRequest;
import com.bookstore.security.CustomUserDetails;
import com.bookstore.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartPageController {
    private final CartService cartService;

    @GetMapping
    public String cart(Authentication authentication, Model model) {
        model.addAttribute("cart", cartService.getCart(userId(authentication)));
        return "cart";
    }

    @PostMapping("/items")
    public String add(@RequestParam Long bookId, @RequestParam(defaultValue = "1") Integer quantity,
                      Authentication authentication) {
        cartService.addItem(userId(authentication), CartItemRequest.builder().bookId(bookId).quantity(quantity).build());
        return "redirect:/cart";
    }

    @PostMapping("/items/{bookId}/remove")
    public String remove(@PathVariable Long bookId, Authentication authentication) {
        cartService.removeItem(userId(authentication), bookId);
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clear(Authentication authentication) {
        cartService.clearCart(userId(authentication));
        return "redirect:/cart";
    }

    private Long userId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
    }
}
