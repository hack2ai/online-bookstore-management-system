package com.bookstore.repository;

import com.bookstore.entity.Order;
import com.bookstore.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * "View order history" for a customer — always scoped to their own
     * {@code userId}. {@code OrderServiceImpl} additionally verifies, when
     * fetching a single order by ID, that {@code order.getUser().getId()}
     * matches the requester unless they're an admin, so a customer can
     * never reach another customer's order even by guessing an ID directly
     * against {@code GET /api/orders/{id}}.
     */
    Page<Order> findByUserId(Long userId, Pageable pageable);

    /** Backs admin order management's "filter by status" view. */
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    long countByStatus(OrderStatus status);
}
