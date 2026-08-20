# Online Bookstore Management System

A production-oriented full-stack bookstore platform built with Java 21 and Spring Boot. Customers can browse the catalog, manage carts, use wishlists, review purchased books, apply coupons, checkout, place orders and pay through Razorpay; administrators manage the catalog, inventory, customers, orders and analytics.

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
- **Testing:** Spring Boot Test · Spring Security Test · Mockito · H2 test profile
- **CI/CD:** GitHub Actions · Docker · Docker Compose

## Current Features

### Authentication & security
- Customer/admin roles
- JWT authentication
- Refresh-token rotation and revocation with hashed persisted refresh tokens
- BCrypt password hashing
- Method-level authorization
- Stateless API security
- Customer/admin ownership boundaries for carts, orders and reviews

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
- Wishlist management

### Orders
- Checkout from cart
- Frozen historical item prices
- Shipping-address snapshot
- Coupon-aware subtotal/discount/final-total snapshots
- Atomic inventory updates with pessimistic row locking
- Customer order history
- Order cancellation with inventory restoration
- Admin status management with validated state transitions

### Coupons
- Percentage and fixed-value discounts
- Minimum order amount
- Maximum discount cap
- Start/expiry dates
- Usage limits
- One-use-per-customer enforcement
- Transaction-safe coupon reservation/release lifecycle

### Reviews & ratings
- Ratings from 1 to 5
- Review comments with validation
- One review per customer/book
- Verified-purchase requirement
- Review ownership protection
- Average rating and review count

### Payments
- Razorpay order creation
- Server-side signature verification
- Payment state tracking
- Idempotent successful verification
- Cancelled orders cannot be paid

### Admin
- Dashboard metrics
- Revenue / paid-order analytics
- Book management
- Category management
- Inventory monitoring and low-stock visibility
- Customer management with order/spending aggregates
- Order status operations

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

GET    /api/wishlist              CUSTOMER
POST   /api/wishlist/{bookId}     CUSTOMER
DELETE /api/wishlist/{bookId}     CUSTOMER

GET    /api/books/{bookId}/reviews
GET    /api/books/{bookId}/reviews/summary
POST   /api/books/{bookId}/reviews          CUSTOMER
DELETE /api/books/{bookId}/reviews/{id}     CUSTOMER

POST   /api/coupons/validate      CUSTOMER

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
- Docker + Docker Compose for containerized development

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

The test suite covers authentication, role separation, carts, checkout, coupons, payment idempotency, cancellation/restocking, reviews, and API error mapping.

### Docker Compose

```bash
docker compose up --build
```

Compose starts MySQL and the bookstore application with a healthcheck dependency. For production, provide secrets through the environment rather than committing them to the repository.

## Production

Use the `prod` profile:

```bash
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
```

Production requires environment variables for database credentials, JWT secret and Razorpay credentials. The production profile disables automatic `schema.sql` initialization; database schema changes should be managed through a real migration process before production deployment.

Never commit real credentials, API keys or JWT secrets.

## CI

GitHub Actions runs the Maven test suite and packages the application artifact on pushes and pull requests. A real deployment should add environment-specific secrets and an explicit deployment job after CI is consistently green.

## Engineering Principles

- Controllers remain thin.
- Business rules live in services.
- JPA entities are never exposed as public API contracts.
- State-changing workflows are transactional.
- Inventory checkout uses database row locking.
- Historical order prices are immutable snapshots.
- Database constraints remain the final integrity boundary.
- Coupon usage is concurrency-aware and tied to payment/order lifecycle.
- Production secrets come from the environment.
- Every phase should remain buildable and testable before merging to `main`.
