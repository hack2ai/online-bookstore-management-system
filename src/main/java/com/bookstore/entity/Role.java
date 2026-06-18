package com.bookstore.entity;

/**
 * Application-level user roles.
 *
 * <p>Stored on {@link User#role} and used to build Spring Security
 * {@code GrantedAuthority}s as {@code "ROLE_" + name()} (e.g. {@code ROLE_ADMIN}).
 * Kept as a Java enum (mapped via {@code @Enumerated(EnumType.STRING)}) rather than
 * a free-text column so invalid roles are rejected at the JPA layer, not just by
 * application code that happens to remember to validate them.
 */
public enum Role {
    CUSTOMER,
    ADMIN
}
