# Production Deployment Runbook

This runbook deploys the bookstore as a Docker Compose application with MySQL 8.4 and the Spring Boot production profile.

## 1. Prerequisites

- Docker Engine / Docker Desktop with Docker Compose
- A host with persistent storage for MySQL
- A DNS name and HTTPS reverse proxy for production traffic
- Production database credentials
- A strong random JWT secret
- Razorpay production credentials

## 2. Configure environment

Create a `.env` file beside `docker-compose.yml`.

Required variables:

```text
DB_USERNAME=<strong-database-user>
DB_PASSWORD=<strong-database-password>
DB_ROOT_PASSWORD=<strong-root-password>
JWT_SECRET=<strong-random-secret>
RAZORPAY_KEY_ID=<production-key-id>
RAZORPAY_KEY_SECRET=<production-key-secret>
```

Do not commit `.env`. Keep credentials in the deployment platform's secret store when one is available.

## 3. Start the application

```bash
docker compose up -d --build
```

The application container waits for a healthy MySQL container before starting. MySQL is intentionally not published to the host by the production Compose configuration; the application connects to it over the private Compose network.

## 4. Verify health

Check container state:

```bash
docker compose ps
```

Check the Spring Boot health endpoint:

```bash
curl --fail http://127.0.0.1:8080/actuator/health
```

Expected response:

```json
{"status":"UP"}
```

The image and Compose configuration use the same health endpoint for container health checks.

## 5. Put HTTPS in front of the application

Terminate TLS at a reverse proxy or managed load balancer and forward traffic to the application container. The application enables HSTS in its security headers, so production traffic should be served over HTTPS. Keep the application port private to the host/network where possible and expose only the HTTPS reverse-proxy entry point publicly.

## 6. Database persistence and backups

The Compose deployment stores MySQL data in the `mysql_data` named volume. Back up that volume/database according to your operational recovery policy before upgrades or migrations.

Schema ownership is handled by Flyway, while Hibernate runs in validation mode in production. Do not manually modify the production schema outside the migration process.

## 7. Deploy updates

Pull the new application revision, rebuild the image, and recreate the services:

```bash
git pull origin main
docker compose up -d --build
```

Then verify:

```bash
docker compose ps
curl --fail http://127.0.0.1:8080/actuator/health
```

Review application logs after each deployment:

```bash
docker compose logs --tail=200 app
```

## 8. Production security checklist

- Use unique, strong database credentials.
- Use a high-entropy JWT secret and never store it in Git.
- Store Razorpay credentials in a secret manager or protected environment configuration.
- Keep MySQL private to the Compose network.
- Serve the application through HTTPS.
- Restrict inbound network access to the required web ports.
- Keep the host, Docker engine, base images and dependencies patched.
- Protect and regularly test database backups.
- Monitor `/actuator/health` and application logs.
- Run CI before every production release.

The repository's `.env.example` provides the variable names and safe placeholder values for local configuration.
