package com.bookstore.repository;

import com.bookstore.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Backs the "adding an already-present book increments quantity instead
     * of inserting a duplicate row" rule described on {@link CartItem}.
     * {@code CartServiceImpl#addToCart} calls this first and only inserts a
     * new row if it comes back empty.
     */
    Optional<CartItem> findByCartIdAndBookId(Long cartId, Long bookId);
}
