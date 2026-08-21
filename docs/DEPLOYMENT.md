# Deployment Runbook

## 1. Local Docker deployment

Copy the example environment file and replace every placeholder with real values.

```bash
cp .env.example .env
```

Start the application and MySQL:

```bash
docker compose up --build -d
```

Check container status:

```bash
docker compose ps
```

The application is available at `http://localhost:8080`.
Health endpoint:

```text
GET /actuator/health
```

Stop the stack:

```bash
docker compose down
```

Persistent MySQL data is stored in the `mysql_data` Docker volume.

## 2. Production secrets

Required values:

- `DB_HOST`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `RAZORPAY_KEY_ID`
- `RAZORPAY_KEY_SECRET`

Use a secret manager or the deployment platform's encrypted environment variables. Never commit `.env` or live secrets.

## 3. Production database

The production profile uses Hibernate schema validation and disables startup `schema.sql` initialization. Run a versioned database migration process before deploying schema changes. Flyway or Liquibase is recommended for production operations.

## 4. Release procedure

1. Open a pull request into `main`.
2. Wait for GitHub Actions to pass tests and package the application.
3. Build the production container from the reviewed commit.
4. Apply database migrations.
5. Deploy with the production environment variables.
6. Verify `/actuator/health`.
7. Verify login, catalog browsing and a payment test-mode transaction before enabling live payment credentials.

## 5. Rollback

Rollback the application image to the previous known-good commit/image. Database migrations must be backward-compatible or have a documented down-migration strategy before rollout.

## 6. Operational checks

After deployment, verify:

```text
GET /actuator/health
GET /api/books
POST /api/auth/login
GET /swagger-ui.html
```

Then inspect application logs for database connectivity, authentication failures and payment verification errors.
