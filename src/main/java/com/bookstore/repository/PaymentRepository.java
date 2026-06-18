package com.bookstore.repository;

import com.bookstore.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    /**
     * Used when the gateway calls back referencing its own transaction
     * identifier (Razorpay's {@code order_id}/{@code payment_id} in live
     * mode, or the {@code "MOCK-" + UUID} value in mock mode) rather than
     * our internal {@code Payment.id} — see {@code PaymentServiceImpl#verifyPayment}.
     */
    Optional<Payment> findByTransactionId(String transactionId);
}
