package com.bookstore.controller;

import com.bookstore.dto.response.OrderResponse;
import com.bookstore.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class PaymentPageController {
    private final OrderService orderService;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @GetMapping("/orders/{orderId}/payment")
    public String payment(@PathVariable Long orderId, Authentication authentication, Model model) {
        var principal = (com.bookstore.security.CustomUserDetails) authentication.getPrincipal();
        OrderResponse order = orderService.getMyOrder(principal.getUser().getId(), orderId);
        model.addAttribute("order", order);
        model.addAttribute("razorpayKeyId", razorpayKeyId);
        return "payment";
    }
}
