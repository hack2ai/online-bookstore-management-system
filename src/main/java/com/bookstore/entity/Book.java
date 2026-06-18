package com.bookstore.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A book listed in the catalog.
 *
 * <p>{@code category} is mapped as a real {@code @ManyToOne} (not a bare
 * {@code Long category_id} field) so JPQL/derived-query joins, lazy loading,
 * and referential integrity (a category can't be deleted while books still
 * reference it — see {@code CategoryServiceImpl}) all work the way they're
 * supposed to. The underlying column is still named {@code category_id} so
 * the physical schema matches the spec exactly.
 *
 * <p>{@code price} is {@link BigDecimal}, never {@code double} — money fields
 * must not use binary floating point, since rounding error in prices/totals
 * is exactly the kind of bug that's invisible in testing and expensive in
 * production. The same applies to {@code Order.totalAmount} and
 * {@code OrderItem.price} later.
 */
@Entity
@Table(name = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String title;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String author;

    @NotBlank
    @Column(nullable = false, unique = true, length = 20)
    private String isbn;

    @Column(length = 2000)
    private String description;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true, message = "price must not be negative")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @NotNull
    @Min(value = 0, message = "stock must not be negative")
    @Column(nullable = false)
    @Builder.Default
    private Integer stock = 0;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_books_category"))
    private Category category;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
