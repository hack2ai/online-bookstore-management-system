package com.bookstore.repository;

import com.bookstore.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * The primary lookup used everywhere: every cart operation starts from
     * "get this user's cart" (creating one if absent — see
     * {@code CartServiceImpl#getOrCreateCart}), never from a bare cart ID,
     * since a customer should never be able to address another customer's
     * cart by guessing its ID.
     */
    Optional<Cart> findByUserId(Long userId);
}
