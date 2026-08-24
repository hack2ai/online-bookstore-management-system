# Deployment Runbook

This runbook covers the supported Docker Compose deployment path for the bookstore application.

## 1. Prerequisites

Install Docker Desktop with Docker Compose support. For non-container development, the repository also supports Java 21, Maven 3.8+, and MySQL 8.

## 2. Configure the environment

Create a local environment file from the checked-in template:

```bash
copy .env.example .env
```

Replace all placeholders before starting the stack. Never commit `.env`, live credentials, API keys, or JWT secrets.

Current configuration names include:

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
- `DB_ROOT_PASSWORD`
- `SPRING_PROFILES_ACTIVE`
- `JWT_SECRET`, `JWT_EXPIRATION_MS`, `JWT_REFRESH_EXPIRATION_MS`, `JWT_ISSUER`
- `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_CURRENCY`
- `UPLOAD_DIR`

The repository `.env.example` is the source of truth for the current variable names and placeholder values.

## 3. Start the stack

From the repository root:

```bash
docker compose up --build -d
```

The Compose stack starts MySQL and waits for the database health condition before starting the application.

## 4. Verify the deployment

Check the containers:

```bash
docker compose ps
```

After startup settles, both `bookstore-mysql` and `bookstore-app` should report `healthy`.

Check the application health endpoint:

```bash
curl -i http://localhost:8080/actuator/health
```

Expected:

```text
HTTP/1.1 200

{"status":"UP"}
```

Check the storefront:

```bash
curl -i http://localhost:8080/
```

## 5. Logs and diagnostics

Application logs:

```bash
docker compose logs app --tail=200
```

MySQL logs:

```bash
docker compose logs mysql --tail=200
```

For a container-local health check:

```bash
docker compose exec app curl -i http://127.0.0.1:8080/actuator/health
```

## 6. Restart and rebuild

Restart without deleting persistent database data:

```bash
docker compose restart
```

Rebuild after source or dependency changes:

```bash
docker compose down
docker compose up --build -d
```

Do **not** use `docker compose down -v` unless you intentionally want to delete the local `mysql_data` volume.

## 7. Database migrations

Flyway owns schema migrations. The production profile uses Hibernate schema validation, so production schema changes should be introduced through versioned Flyway migrations rather than automatic Hibernate DDL generation.

Before deploying a schema-changing release, review the migration against the current database version and back up production data according to your operational policy.

## 8. Troubleshooting

### App is unhealthy or restarting

Run:

```bash
docker compose ps -a
docker compose logs app --tail=200
```

Then inspect the health history:

```bash
docker inspect --format="{{json .State.Health}}" bookstore-app
```

Look for Spring startup exceptions, configuration errors, failed database connections, and failing healthcheck commands.

### Database is unhealthy

Run:

```bash
docker compose logs mysql --tail=200
```

Verify that the credentials used by the application match the MySQL container configuration.

### Host port 8080 is busy

Stop the conflicting process or change the host-side port mapping in `docker-compose.yml`.

### Host port 3306 is busy

Stop the conflicting local MySQL service or change the host-side mapping when direct database access is required.

## 9. Production safety checklist

Before exposing the application beyond a trusted local environment:

- Use a strong random `JWT_SECRET`.
- Use non-default database credentials.
- Supply real Razorpay credentials only through protected deployment secrets.
- Keep `.env` out of version control.
- Confirm `/actuator/health` returns HTTP 200 after startup.
- Confirm protected admin routes are not anonymously accessible.
- Back up the database before destructive maintenance.
- Review deployment logs for migration failures, authentication errors, or payment verification failures.

## 10. Release procedure

1. Open a pull request into the repository's integration branch.
2. Wait for GitHub Actions checks to pass.
3. Build the production container from the reviewed commit.
4. Apply database migrations through the application startup process.
5. Deploy with the production environment variables.
6. Verify `/actuator/health`.
7. Verify storefront access, catalog browsing, authentication, and a payment test-mode flow when payment integration is enabled.
8. Record the deployed Git tag or commit for rollback.

## 11. Rollback

Redeploy the previous known-good Git tag or container image:

```bash
git checkout <known-good-tag-or-commit>
docker compose down
docker compose up --build -d
```

Verify `/actuator/health` and the storefront before considering the rollback complete.

Database rollback must be handled separately. Prefer backward-compatible migrations and a documented recovery procedure for production schema changes.
