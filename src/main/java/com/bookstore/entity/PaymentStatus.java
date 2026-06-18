package com.bookstore.entity;

/**
 * Status of a {@link Payment} attempt against the gateway (Razorpay or mock).
 */
public enum PaymentStatus {
    CREATED,
    SUCCESS,
    FAILED,
    REFUNDED
}
