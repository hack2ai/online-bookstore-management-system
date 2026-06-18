package com.bookstore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * A customer's shopping cart — exactly one per {@link User}.
 *
 * <p>Created lazily: a user has no {@code Cart} row at all until their first
 * "add to cart" call (see {@code CartServiceImpl#getOrCreateCart}), rather than
 * one being inserted at registration time for every user who may never buy
 * anything.
 *
 * <p>{@code items} cascades {@code ALL} with {@code orphanRemoval = true}: removing
 * a {@link CartItem} from this list and saving the cart deletes that row outright,
 * which is exactly the semantics "remove from cart" needs.
 */
@Entity
@Table(name = "cart")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "items"}) // avoid recursive toString() through the bidirectional links
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_cart_user"))
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    /**
     * Convenience helper that keeps both sides of the bidirectional
     * {@code Cart <-> CartItem} association in sync, used by
     * {@code CartServiceImpl} instead of callers manually pushing onto
     * {@code items} and forgetting to set {@code item.setCart(this)}.
     */
    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
    }

    public void removeItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
    }
}
