package com.bookstore.repository;

import com.bookstore.entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Backs the "Best Selling Books" admin report (built out in Stage 4):
     * total quantity sold per book across every order, highest first.
     * Returns {@code Object[]} rows of {@code [Book, Long totalSold]} —
     * {@code ReportServiceImpl} maps these into a proper response DTO
     * rather than the controller layer touching raw query results.
     *
     * <p>Deliberately does NOT filter by {@code Order.status}: a book that
     * was ordered and later cancelled was still, at minimum, "selected" by a
     * customer, and excluding cancelled orders here is a business-policy
     * choice better made explicitly in the service layer (where it's easy
     * to change) than baked silently into this query.
     */
    @Query("""
            SELECT oi.book, SUM(oi.quantity) AS totalSold
            FROM OrderItem oi
            GROUP BY oi.book
            ORDER BY totalSold DESC
            """)
    List<Object[]> findBestSellingBooks(Pageable pageable);
}
