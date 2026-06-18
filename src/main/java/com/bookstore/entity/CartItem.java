package com.bookstore.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A single line in a {@link Cart}: one book and the quantity of it requested.
 *
 * <p>The unique constraint on {@code (cart_id, book_id)} means "add to cart"
 * for a book already present must be implemented as an update (increment
 * {@code quantity}) rather than an insert — see
 * {@code CartServiceImpl#addToCart}. Without this constraint a buggy client
 * retry could silently create two rows for the same book in one cart, and
 * the cart total would double-count it.
 */
@Entity
@Table(
        name = "cart_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_cart_items_cart_book", columnNames = {"cart_id", "book_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"cart", "book"})
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cart_items_cart"))
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cart_items_book"))
    private Book book;

    @NotNull
    @Min(value = 1, message = "quantity must be at least 1")
    @Column(nullable = false)
    private Integer quantity;
}
