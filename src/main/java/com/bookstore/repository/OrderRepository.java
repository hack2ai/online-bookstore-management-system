package com.bookstore.repository;

import com.bookstore.entity.Order;
import com.bookstore.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUserId(Long userId, Pageable pageable);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    long countByStatus(OrderStatus status);
    long countByUserId(Long userId);

    @Query("select coalesce(sum(o.totalAmount), 0) from Order o join o.payment p where o.user.id = :userId and p.paymentStatus = com.bookstore.entity.PaymentStatus.SUCCESS")
    BigDecimal sumPaidAmountByUserId(@Param("userId") Long userId);

    @Query("select coalesce(sum(o.totalAmount), 0) from Order o join o.payment p where p.paymentStatus = com.bookstore.entity.PaymentStatus.SUCCESS")
    BigDecimal sumAllPaidAmount();

    @Query("select count(o) > 0 from Order o join o.orderItems i join o.payment p where o.user.id = :userId and i.book.id = :bookId and p.paymentStatus = com.bookstore.entity.PaymentStatus.SUCCESS")
    boolean hasSuccessfulPurchaseOfBook(@Param("userId") Long userId, @Param("bookId") Long bookId);

    long countByStatusAndOrderDateGreaterThanEqualAndOrderDateLessThan(
            OrderStatus status, LocalDateTime from, LocalDateTime to);

    @Query("select coalesce(sum(o.totalAmount), 0) from Order o join o.payment p " +
           "where p.paymentStatus = com.bookstore.entity.PaymentStatus.SUCCESS " +
           "and o.orderDate >= :from and o.orderDate < :to")
    BigDecimal sumPaidAmountBetween(@Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to);

    @Query("select count(o) from Order o join o.payment p " +
           "where p.paymentStatus = com.bookstore.entity.PaymentStatus.SUCCESS " +
           "and o.orderDate >= :from and o.orderDate < :to")
    long countPaidOrdersBetween(@Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to);
}
