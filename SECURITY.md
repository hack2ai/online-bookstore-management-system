# Security Policy

## Supported versions

Security fixes are developed and released against the active `main` branch. Older releases may receive fixes only when explicitly maintained by the project.

## Reporting a vulnerability

Please do not open a public issue for a suspected security vulnerability.

Instead, contact the repository maintainers privately with:

- a clear description of the vulnerability
- affected endpoint or component
- reproduction steps or a minimal proof of concept
- impact assessment, if known

Never include real passwords, JWT secrets, Razorpay credentials, database credentials, or customer data in an issue or pull request.

## Secret handling

- Store runtime secrets in environment variables or a secret manager.
- Never commit `.env` files, production JWT secrets, database passwords, or payment credentials.
- Use a long, random JWT secret in every non-development environment.
- Rotate credentials after suspected exposure.
- Use test Razorpay credentials for local development and CI.

## Authentication and authorization

The application uses Spring Security, BCrypt password hashing, JWT access/refresh tokens, and role-based authorization. Authentication endpoints are rate-limited as a defense-in-depth control. Admin-only operations must remain protected by server-side authorization; UI visibility is not a security boundary.

## Production checklist

Before deployment:

1. Set unique database credentials.
2. Set a random JWT secret of at least 32 bytes.
3. Configure real Razorpay credentials only through the deployment secret store.
4. Use HTTPS at the edge/reverse proxy.
5. Allow Flyway to apply the versioned database migrations during application startup and keep Hibernate schema validation enabled.
6. Disable or restrict development/demo accounts and data.
7. Verify `/actuator/health` is reachable only where appropriate for the deployment environment.
8. Confirm backups, monitoring, and log retention are configured.
9. Run the full CI test suite before release.
10. Follow `DEPLOYMENT.md` for the production deployment and rollback procedure.
