package com.bookstore.controller;

import com.bookstore.dto.response.OrderResponse;
import com.bookstore.dto.response.PaymentResponse;
import com.bookstore.security.CustomUserDetails;
import com.bookstore.service.OrderService;
import com.bookstore.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderPageController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    @Value("${razorpay.key-id:}")
    private String razorpayKeyId;

    @GetMapping
    public String orders(Authentication authentication,
                         @PageableDefault(size = 20, sort = "orderDate") Pageable pageable,
                         Model model) {
        Page<OrderResponse> orders = orderService.getMyOrders(userId(authentication), pageable);
        model.addAttribute("orders", orders.getContent());
        return "orders";
    }

    @GetMapping("/{id}")
    public String order(@PathVariable Long id, Authentication authentication, Model model) {
        model.addAttribute("order", orderService.getMyOrder(userId(authentication), id));
        return "order-details";
    }

    @GetMapping("/{id}/pay")
    public String pay(@PathVariable Long id, Authentication authentication, Model model) {
        Long userId = userId(authentication);
        OrderResponse order = orderService.getMyOrder(userId, id);
        PaymentResponse payment = paymentService.createPayment(userId, id);
        model.addAttribute("order", order);
        model.addAttribute("payment", payment);
        model.addAttribute("razorpayKeyId", razorpayKeyId);
        return "payment";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        orderService.cancelOrder(userId(authentication), id);
        redirectAttributes.addFlashAttribute("success", "Order cancelled successfully.");
        return "redirect:/orders/" + id;
    }

    private Long userId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
    }
}
