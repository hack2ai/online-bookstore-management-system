package com.bookstore.controller;

import com.bookstore.dto.request.CheckoutRequest;
import com.bookstore.dto.response.CartResponse;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutPageController {

    private final CartService cartService;
    private final OrderService orderService;

    @GetMapping
    public String checkout(Authentication authentication,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        CartResponse cart = cartService.getCart(userId(authentication));
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            redirectAttributes.addFlashAttribute("warning", "Your cart is empty. Add a book before checking out.");
            return "redirect:/cart";
        }

        model.addAttribute("cart", cart);
        return "checkout";
    }

    @PostMapping
    public String placeOrder(@Valid CheckoutRequest request,
                             BindingResult bindingResult,
                             Authentication authentication,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        CartResponse cart = cartService.getCart(userId(authentication));
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            redirectAttributes.addFlashAttribute("warning", "Your cart is empty. Add a book before checking out.");
            return "redirect:/cart";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("cart", cart);
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
