package com.bookstore.repository;

import com.bookstore.entity.Payment;
import com.bookstore.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(Long orderId);
    Optional<Payment> findByTransactionId(String transactionId);
    long countByPaymentStatus(PaymentStatus paymentStatus);

    @Query("select coalesce(sum(p.order.totalAmount), 0) from Payment p where p.paymentStatus = :status")
    BigDecimal sumOrderAmountsByStatus(PaymentStatus status);
}
