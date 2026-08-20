package com.bookstore.controller;

import com.bookstore.dto.request.CheckoutRequest;
import com.bookstore.security.CustomUserDetails;
import com.bookstore.service.CartService;
import com.bookstore.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutPageController {

    private final CartService cartService;
    private final OrderService orderService;

    @GetMapping
    public String checkout(Authentication authentication, Model model) {
        model.addAttribute("cart", cartService.getCart(userId(authentication)));
        return "checkout";
    }

    @PostMapping
    public String placeOrder(@Valid CheckoutRequest request,
                             BindingResult bindingResult,
                             Authentication authentication,
                             Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("cart", cartService.getCart(userId(authentication)));
            return "checkout";
        }

        var order = orderService.placeOrder(userId(authentication), request);
        model.addAttribute("order", order);
        return "order-success";
    }

    private Long userId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
    }
}
