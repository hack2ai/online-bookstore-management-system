package com.bookstore.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * A single line in an {@link Order}: one book, the quantity ordered, and the
 * price actually paid per unit.
 *
 * <p><b>{@code price} is a frozen snapshot of {@code Book.price} at the moment
 * the order was placed</b> — copied in {@code OrderServiceImpl#placeOrder} and
 * never updated afterwards. This is deliberate, not an oversight: if a book's
 * price changes next month, every customer's past order history must keep
 * showing what they actually paid, not be silently rewritten to today's
 * price. Display code must always read this column, never join back to
 * {@code book.getPrice()} for historical totals.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"order", "book"})
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_order_items_order"))
    private Order order;

    /**
     * Intentionally NOT cascaded and NOT deleted if the order is deleted —
     * the book itself is a separate catalog entity that outlives any one order.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false, foreignKey = @ForeignKey(name = "fk_order_items_book"))
    private Book book;

    @NotNull
    @Min(value = 1, message = "quantity must be at least 1")
    @Column(nullable = false)
    private Integer quantity;

    /** Snapshot of the per-unit price at purchase time. See class javadoc. */
    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
}
