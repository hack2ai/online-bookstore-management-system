# Changelog

All notable changes to this project are documented here.

## [1.0.0] — 2026-08-21

### Added
- Customer and administrator authentication with role-based access control.
- JWT access tokens with rotating persisted refresh tokens.
- Customer account, profile editing and password-management pages.
- Public catalog browsing, search, categories and pagination.
- Customer carts, checkout and order history.
- Wishlist management.
- Coupons with usage and lifecycle rules.
- Verified customer reviews and ratings.
- Razorpay payment order creation and server-side signature verification.
- Customer order cancellation and inventory restoration.
- Admin catalog, inventory, customer, order and analytics dashboards.
- Custom 403, 404 and 500 error pages.
- Flyway database migrations with Hibernate schema validation.
- Production Docker healthchecks and MySQL startup ordering.
- Production environment configuration with `.env.example` placeholders.
- GitHub Actions CI for tests, packaging, artifact verification and Docker image builds.

### Verified
- 30 automated tests passing with 0 failures and 0 errors.
- Docker application and MySQL containers healthy.
- `/actuator/health` returns `{"status":"UP"}`.
- Flyway baseline successfully registered and existing database data preserved.

## Release Notes

`v1.0.0` marks the completion of the professionalization pass for the bookstore platform, including customer/admin workflows, payment integration, operational dashboards, database migration management, container health checks, CI, testing and production-oriented configuration.
