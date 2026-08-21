# Architecture

## Overview

The application is a layered Spring Boot bookstore platform with a server-rendered Thymeleaf UI and a stateless REST API.

```text
                    ┌──────────────────────┐
                    │      Browser/UI       │
                    │ Thymeleaf + Bootstrap │
                    └──────────┬───────────┘
                               │
                    ┌──────────▼───────────┐
                    │   Controller Layer   │
                    │ REST + MVC + Admin   │
                    └──────────┬───────────┘
                               │
                    ┌──────────▼───────────┐
                    │    Service Layer     │
                    │ Business transactions│
                    └──────────┬───────────┘
                               │
               ┌──────────────▼──────────────┐
               │       Repository Layer      │
               │ Spring Data JPA + locking   │
               └──────────────┬──────────────┘
                              │
                    ┌─────────▼─────────┐
                    │       MySQL       │
                    └───────────────────┘

External integrations:
  Razorpay ← PaymentService
  GitHub Actions ← CI
  Docker / Compose ← deployment runtime
```

## Security model

The REST API is stateless. Access tokens are JWTs; refresh tokens are random opaque values whose SHA-256 hashes are stored server-side. Refresh rotates the stored token so a previously used refresh token is rejected.

Role boundaries:

- `ROLE_CUSTOMER`: cart, checkout, customer orders, wishlist and customer reviews.
- `ROLE_ADMIN`: catalog administration, inventory, order administration, customers and analytics.
- Public: catalog reads and authentication bootstrap endpoints.

## Transaction boundaries

The highest-risk state-changing workflows are transactional:

### Checkout

```text
Load cart
  ↓
Lock each book row
  ↓
Validate stock
  ↓
Snapshot unit price into OrderItem
  ↓
Calculate subtotal
  ↓
Reserve coupon (when supplied)
  ↓
Persist order
  ↓
Clear cart
```

The pessimistic row lock prevents concurrent checkouts from observing stale stock.

### Cancellation

```text
Find owned order
  ↓
Validate status transition
  ↓
Lock each book row
  ↓
Restore stock
  ↓
Release pending coupon reservation
  ↓
Mark order CANCELLED
```

### Payment

```text
Create server-side Razorpay order
  ↓
Browser opens Razorpay Checkout
  ↓
Browser returns payment IDs/signature
  ↓
Server verifies signature with secret
  ↓
Payment SUCCESS
  ↓
Pending order → CONFIRMED
```

## Data ownership

Controllers do not calculate business totals or manipulate persistence directly. Services own business rules and transaction boundaries. Repositories own database queries and locking. DTOs define the public API contract; JPA entities stay internal to the persistence layer.

## Production evolution

The current repository uses `schema.sql` for a reproducible baseline and Hibernate `validate`. For a long-lived production system, replace startup schema initialization with Flyway or Liquibase migrations so schema changes become versioned and reversible.
