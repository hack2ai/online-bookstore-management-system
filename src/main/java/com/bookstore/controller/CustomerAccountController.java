package com.bookstore.controller;

import com.bookstore.security.CustomUserDetails;
import com.bookstore.service.CustomerAccountService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerAccountController {

    private final CustomerAccountService accountService;
    private final OrderService orderService;
    private final WishlistService wishlistService;

    @GetMapping
    public String account(Authentication authentication, Model model) {
        Long userId = userId(authentication);
        var user = accountService.getUser(userId);
        var orders = orderService.getMyOrders(
                userId,
                PageRequest.of(0, 5, Sort.by("orderDate").descending())
        );
        model.addAttribute("user", user);
        model.addAttribute("orders", orders);
        model.addAttribute("wishlistCount", wishlistService.getWishlist(userId).size());
        return "account";
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        model.addAttribute("user", accountService.getUser(userId(authentication)));
        return "account-profile";
    }

    @PostMapping("/profile")
    public String updateProfile(Authentication authentication,
                                @RequestParam String name,
                                @RequestParam(required = false) String phone,
                                RedirectAttributes redirectAttributes) {
        try {
            accountService.updateProfile(userId(authentication), name, phone);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully.");
            return "redirect:/account/profile";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/account/profile";
        }
    }

    @GetMapping("/security")
    public String security() {
        return "account-security";
    }

    @PostMapping("/security")
    public String changePassword(Authentication authentication,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        try {
            accountService.changePassword(userId(authentication), currentPassword, newPassword, confirmPassword);
            redirectAttributes.addFlashAttribute("success", "Password changed successfully.");
            return "redirect:/account/security";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/account/security";
        }
    }

    private Long userId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
    }
}
