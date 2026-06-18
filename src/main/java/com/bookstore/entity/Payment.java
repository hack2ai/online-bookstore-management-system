package com.bookstore.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * The payment attempt/result for exactly one {@link Order}.
 *
 * <p>One row per order (not per attempt): if a payment fails and the customer
 * retries, {@code PaymentServiceImpl} updates this same row's
 * {@code status}/{@code transactionId} rather than inserting a second row,
 * since the spec's schema has no order-to-many-payments relationship and a
 * 1:1 keeps "what did this order's payment end up being" a single
 * unambiguous lookup.
 *
 * <p>{@code transactionId} comes from Razorpay's {@code order_id} /
 * {@code payment_id} in live mode, or a generated {@code "MOCK-" + UUID}
 * value when running in mock mode (see {@code PaymentGatewayService},
 * added in Stage 4) — either way it's an opaque external reference, not
 * something this entity interprets.
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "order")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_payments_order"))
    private Order order;

    @NotBlank
    @Column(name = "payment_method", nullable = false, length = 30)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.CREATED;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;
}
