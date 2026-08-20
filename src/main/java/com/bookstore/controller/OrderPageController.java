package com.bookstore.controller;

import com.bookstore.security.CustomUserDetails;
import com.bookstore.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderPageController {

    private final OrderService orderService;

    @GetMapping
    public String orders(Authentication authentication,
                         @PageableDefault(size = 20, sort = "orderDate") Pageable pageable,
                         Model model) {
        Page<?> orders = orderService.getMyOrders(userId(authentication), pageable);
        model.addAttribute("orders", orders);
        return "orders";
    }

    private Long userId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
    }
}
