package com.bookstore.controller;

import com.bookstore.security.CustomUserDetails;
import com.bookstore.service.OrderService;
import com.bookstore.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerAccountController {

    private final OrderService orderService;
    private final WishlistService wishlistService;

    @GetMapping
    public String account(Authentication authentication, Model model) {
        var user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        var orders = orderService.getMyOrders(
                user.getId(),
                PageRequest.of(0, 5, Sort.by("orderDate").descending())
        );
        model.addAttribute("user", user);
        model.addAttribute("orders", orders);
        model.addAttribute("wishlistCount", wishlistService.getWishlist(user.getId()).size());
        return "account";
    }
}
