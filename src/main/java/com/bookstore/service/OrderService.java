package com.bookstore.service;

import com.bookstore.dto.request.CheckoutRequest;
import com.bookstore.dto.response.OrderResponse;
import com.bookstore.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    default OrderResponse placeOrder(Long userId, CheckoutRequest request) {
        return placeOrder(userId, request, null);
    }

    OrderResponse placeOrder(Long userId, CheckoutRequest request, String idempotencyKey);
    Page<OrderResponse> getMyOrders(Long userId, Pageable pageable);
    OrderResponse getMyOrder(Long userId, Long orderId);
    OrderResponse getOrder(Long orderId);
    void cancelOrder(Long userId, Long orderId);
    Page<OrderResponse> getAllOrders(OrderStatus status, Pageable pageable);
    OrderResponse updateStatus(Long orderId, OrderStatus status);
}
