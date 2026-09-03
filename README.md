# Pocket Estate

A small Spring Boot web app for managing self-storage clients, unit occupancy, and monthly payments.

## Run locally

Requires JDK 17+ and Maven 3.9+.

```bash
mvn spring-boot:run
```

Open http://localhost:8080. You will be sent to the sign-in screen before accessing the workspace.

For a new local installation, select **Create an account** on the first screen. Development can use the file-backed H2 database at `./data/storage-desk`; H2 console access is disabled by default.

## Current workflows

- Review revenue, occupancy, active clients, and payment attention items.
- Add a client from the dashboard.
- View recent payments and unit occupancy.
- Review client contact details, assigned unit, monthly rate, and status.
- Add and remove storage units, assign units to clients, and change availability.
- Record and remove monthly payments.
- Sign out from the workspace when finished.
- Change the signed-in account password from the Account menu.
- Keep an audit trail of client, unit, payment, and password changes.
- Add houses, assign tenants, set monthly rent, and record rent payments.

## PostgreSQL setup

Install Docker Desktop, copy the environment template, and start PostgreSQL:

```bash
cp .env.example .env
# Edit .env and replace DATABASE_PASSWORD with a long random value.
docker compose --env-file .env up -d postgres
set -a; source .env; set +a
mvn spring-boot:run
```

The application uses `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` from the environment. Production uses `FLYWAY_ENABLED=true` and `DDL_AUTO=validate`; Flyway applies the versioned schema in `src/main/resources/db/migration` before the application starts.

## Publish as a website

The repository includes a production `Dockerfile` and a Render deployment template in `render.yaml`. The one-time owner setup is to create a Render account, create a managed PostgreSQL database, and provide the database connection values in the service environment. The `DATABASE_URL` must use JDBC format, for example `jdbc:postgresql://host:5432/storage_desk`.

After deployment, send users the website URL. They can select **Create an account**, sign in, and manage their own clients, units, and payments without you creating accounts or entering data for them. Each account is isolated from the others.

Hosting, domain registration, database billing, backups, and the legal responsibility for customer data still belong to the site owner; those cannot be automated by application code.

## Production checklist

This version has working authentication, BCrypt password hashing, CSRF protection, PostgreSQL support, and secure session cookie options. Before deploying publicly:

- Put the app behind HTTPS and set `SESSION_COOKIE_SECURE=true`.
- Use a managed PostgreSQL database with automated encrypted backups and point-in-time recovery.
- Add Flyway or Liquibase migrations before changing the production schema.
- Add password reset, account administration, rate limiting, and role-based permissions.
- Configure monitoring, error tracking, restore drills, and a separate staging environment.
- Do not enable `APP_DEMO_DATA_ENABLED` or the H2 console in production.
