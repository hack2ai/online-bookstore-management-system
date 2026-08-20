# Online Bookstore Management System

A production-oriented full-stack bookstore platform built with Java 21 and Spring Boot. Customers can browse the catalog, manage carts, checkout, place orders and pay through Razorpay; administrators manage books, categories and order workflows.

> **Status: 🚧 Professionalization in progress**
>
> Active development happens on `feature/professional-bookstore-platform`; `main` is kept stable.

## Tech Stack

- **Backend:** Java 21 · Spring Boot 3.3.4 · Spring Security · Spring Data JPA / Hibernate
- **Security:** JWT access tokens · rotating persisted refresh tokens · BCrypt · RBAC
- **Frontend:** Thymeleaf · Bootstrap 5
- **Database:** MySQL 8
- **Payments:** Razorpay
- **API docs:** springdoc OpenAPI / Swagger UI
- **Build:** Maven
- **Testing:** Spring Boot Test · Spring Security Test · H2 test profile
- **CI:** GitHub Actions

## Current Features

### Authentication & security
- Customer/admin roles
- JWT authentication
- Refresh-token rotation and revocation
- BCrypt password hashing
- Method-level authorization
- Stateless API security

### Catalog
- Book CRUD for administrators
- Public book browsing
- Search by title/author
- Category filtering
- Pagination and safe sorting
- Category CRUD with duplicate protection
- Safe category deletion when books still reference it

### Shopping
- Customer-owned carts
- Add/update/remove/clear cart items
- Duplicate cart-item merging
- Stock validation
- BigDecimal price calculations

### Orders
- Checkout from cart
- Frozen historical item prices
- Shipping-address snapshot
- Atomic inventory updates with pessimistic row locking
- Customer order history
- Order cancellation
- Admin status management

### Payments
- Razorpay order creation
- Server-side signature verification
- Payment state tracking
- Repeat successful verification is handled idempotently

## API Overview

```text
POST   /api/auth/register
POST   /api/auth/login
POST   /api/auth/refresh
POST   /api/auth/logout

GET    /api/books
GET    /api/books/{id}
POST   /api/books                 ADMIN
PUT    /api/books/{id}            ADMIN
DELETE /api/books/{id}            ADMIN

GET    /api/categories
GET    /api/categories/{id}
POST   /api/categories            ADMIN
PUT    /api/categories/{id}       ADMIN
DELETE /api/categories/{id}       ADMIN

GET    /api/cart                  CUSTOMER
POST   /api/cart/items            CUSTOMER
PUT    /api/cart/items/{bookId}   CUSTOMER
DELETE /api/cart/items/{bookId}   CUSTOMER
DELETE /api/cart                  CUSTOMER

POST   /api/orders/checkout       CUSTOMER
GET    /api/orders                CUSTOMER
GET    /api/orders/{id}           CUSTOMER
POST   /api/orders/{id}/cancel    CUSTOMER
GET    /api/orders/admin/all      ADMIN
PATCH  /api/orders/{id}/status    ADMIN

POST   /api/payments/orders/{id}          CUSTOMER
POST   /api/payments/orders/{id}/verify   CUSTOMER
```

## Project Structure

```text
src/main/java/com/bookstore
├── controller     # HTTP / REST boundary
├── dto            # Public request/response contracts
├── entity         # JPA domain model
├── repository     # Spring Data persistence
├── service        # Business logic and transactions
├── security       # JWT and Spring Security
├── exception      # Centralized API errors
└── config         # Cross-cutting configuration
```

## Getting Started

### Prerequisites

- Java 21
- Maven 3.8+
- MySQL 8 for normal development

### Development

The default profile is `dev` and reads database/payment/JWT values from environment variables with local development fallbacks.

```bash
mvn clean test
mvn spring-boot:run
```

The application starts on `http://localhost:8080`.

Swagger UI is available at `/swagger-ui.html` and OpenAPI JSON at `/api-docs`.

### Tests

The test profile uses H2 and does not require a running MySQL instance:

```bash
mvn test
```

## Production

Use the `prod` profile:

```bash
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
```

Production requires environment variables for database credentials, JWT secret and Razorpay credentials. The production profile disables automatic `schema.sql` initialization; database schema changes should be managed through a real migration process before production deployment.

Never commit real credentials, API keys or JWT secrets.

## Engineering Principles

- Controllers remain thin.
- Business rules live in services.
- JPA entities are never exposed as public API contracts.
- State-changing workflows are transactional.
- Inventory checkout uses database row locking.
- Historical order prices are immutable snapshots.
- Database constraints remain the final integrity boundary.
- Production secrets come from the environment.
- Every phase should remain buildable and testable before merging to `main`.
