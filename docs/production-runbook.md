# Production Runbook

## 1. Configure secrets

Provide these through the deployment platform or secret manager:

- `DB_USERNAME`
- `DB_PASSWORD`
- `DB_ROOT_PASSWORD` (only where required for database bootstrap/administration)
- `JWT_SECRET`
- `RAZORPAY_KEY_ID`
- `RAZORPAY_KEY_SECRET`

Do not commit `.env` or production credentials.

## 2. Database

Production schema changes should use a migration tool such as Flyway or Liquibase. Do not treat the demo `schema.sql`/`seed-demo.sql` files as a production migration strategy.

Before a release, verify:

- backup completed
- migration reviewed
- rollback plan documented
- indexes and constraints are present

## 3. Application deployment

Build the immutable application image in CI and deploy that image rather than compiling on the production host.

The container runs as the non-root `bookstore` user. Keep the application port behind a TLS-terminating reverse proxy or load balancer.

## 4. Health and observability

Use `/actuator/health` for liveness/readiness monitoring and keep detailed management endpoints restricted to trusted operators.

Monitor:

- request error rate and latency
- database connection failures
- failed payment verification
- order-state transition failures
- low-stock inventory
- authentication failures

## 5. Release verification

Run:

```bash
mvn -B test
mvn -B -DskipTests package
docker build -t bookstore:release .
```

Then perform a smoke test covering:

1. registration/login
2. catalog search
3. cart update
4. checkout
5. payment verification in the configured payment environment
6. order history
7. admin order status transition
8. inventory update
9. health endpoint

## 6. Incident response

For a suspected credential compromise:

1. revoke/rotate the affected credentials
2. rotate JWT secrets if token signing material may be exposed
3. review authentication and payment logs
4. invalidate active sessions as appropriate
5. document the incident and remediation

For application rollback, redeploy the last known-good immutable image and preserve logs/database evidence before making destructive changes.
