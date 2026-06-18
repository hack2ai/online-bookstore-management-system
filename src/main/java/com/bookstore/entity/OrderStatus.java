package com.bookstore.entity;

/**
 * Lifecycle states for an {@link Order}.
 *
 * <p>Linear-ish progression for the happy path is
 * {@code PENDING -> CONFIRMED -> SHIPPED -> DELIVERED}, with {@code CANCELLED}
 * reachable from {@code PENDING} or {@code CONFIRMED} only (enforced in
 * {@code OrderServiceImpl}, not here — entities stay dumb data holders).
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
