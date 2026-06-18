# Online Bookstore Management System

A full-stack bookstore management system — customers browse books, manage a cart, place orders, and pay; admins manage books, categories, customers, and orders.

> **Status: 🚧 Work in progress — Stage 1 of 4 complete.**
> This repo currently contains the data layer only (entities, repositories, schema). Security/auth, the REST API, payments, reports, and the UI are not built yet. See [Progress](#progress) below.

## Tech Stack

**Backend:** Java 21 · Spring Boot 3.3.4 · Spring Security · Spring Data JPA (Hibernate) · JWT (JJWT)
**Frontend:** Thymeleaf · Bootstrap 5
**Database:** MySQL 8
**Payments:** Razorpay
**Docs:** springdoc-openapi (Swagger UI)
**Build:** Maven

## Progress

- [x] **Stage 1 — Foundation:** JPA entities, Spring Data repositories, MySQL schema (`db/schema.sql`), base config
- [ ] **Stage 2 — Security:** Spring Security + JWT auth, role-based authorization, BCrypt
- [ ] **Stage 3 — Core API:** Books, Categories, Cart, Orders — services, controllers, DTOs, validation, global exception handling
- [ ] **Stage 4 — Payments, Reports & UI:** Razorpay integration, sales reports, Thymeleaf pages, Swagger docs

## Project Structure

```
src/main/java/com/bookstore
├── controller     # REST + MVC controllers (Stage 3+)
├── service        # Business logic (Stage 3+)
├── repository     # Spring Data JPA repositories
├── entity         # JPA entities + enums
├── dto            # Request/response DTOs (Stage 3+)
├── security       # JWT + Spring Security config (Stage 2+)
├── config         # App-wide configuration beans
├── exception       # Global exception handling (Stage 3+)
└── util           # Shared helpers
```

## Getting Started

### Prerequisites
- Java 21
- Maven 3.8+
- MySQL 8 running locally (or reachable)

### Configure the database

The app reads connection details from environment variables, with safe local
defaults baked in (see `application.properties`):

| Variable | Default |
|---|---|
| `DB_HOST` | `localhost` |
| `DB_PORT` | `3306` |
| `DB_NAME` | `bookstore_db` |
| `DB_USERNAME` | `root` |
| `DB_PASSWORD` | `root` |

The schema is created automatically on startup from `src/main/resources/db/schema.sql`
(`createDatabaseIfNotExist=true` is set, so the database itself doesn't need to
exist beforehand — just a reachable MySQL server).

### Run

```bash
mvn clean install
mvn spring-boot:run
```

The app starts on `http://localhost:8080`.

### Run tests

Tests run against an in-memory H2 database (no MySQL needed for `mvn test`):

```bash
mvn test
```

## Security Notes

- `app.jwt.secret`, `DB_PASSWORD`, and `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` all have
  insecure local-dev defaults baked in for convenience — **override every one of these
  via environment variables before deploying anywhere real.**
- `spring.jpa.hibernate.ddl-auto=validate` is intentional: `schema.sql` is the single
  source of truth for the schema, and Hibernate will refuse to start if an entity
  mapping disagrees with it. Update both together.
