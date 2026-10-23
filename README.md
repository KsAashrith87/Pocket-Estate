# Pocket Estate

Pocket Estate is an Angular single-page application backed by Spring Boot and MySQL. It provides account registration and sign-in, tenant/client records, storage unit and house inventory, rent/payment tracking, occupancy and vacancy summaries, payment-due notifications, and an audit trail.

## Run locally with Docker

Requires Docker Engine with Docker Compose.

```bash
cp .env.example .env
# Replace DATABASE_ROOT_PASSWORD and DATABASE_PASSWORD with unique, long random values.
docker compose up --build
```

Open http://localhost:8080 and create an account. The MySQL database persists in the `storage-mysql` Docker volume. MySQL is bound to the host loopback interface only. Keep `.env` private and never commit real credentials.

To stop the app without deleting its database:

```bash
docker compose down
```

Do not use `docker compose down -v` unless intentionally deleting every account and all associated business data.

## Angular development server

For frontend hot reload, first start MySQL:

```bash
docker compose up -d mysql
```

Run Spring Boot with the database values from `.env` and `DATABASE_URL=jdbc:mysql://localhost:3306/storage_desk`, then start Angular in another terminal:

```bash
set -a; source .env; set +a
DATABASE_URL=jdbc:mysql://localhost:3306/storage_desk mvn spring-boot:run
```

```bash
cd frontend
npm ci
npm start
```

The Angular dev server listens on port 4200 and proxies `/api` requests to Spring Boot on port 8080. For a packaged application, `npm run build` creates `frontend/dist/browser`; Maven includes those assets in the Spring Boot jar.

## Data and migrations

MySQL 8.4 is the application database. Flyway applies the SQL migrations in `src/main/resources/db/migration`; Hibernate validates the resulting schema at startup. `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` can be overridden for another MySQL host. The supported URL format is `jdbc:mysql://host:3306/storage_desk`.

The old local H2 database was intentionally removed. The test suite uses an isolated in-memory H2 database; it is not used for application data.

## Deploy to AWS EC2

Terraform configuration for the EC2 instance, Elastic IP, HTTPS/HTTP security group, encrypted root volume, and Systems Manager access is in [`deploy/ec2/terraform/`](./deploy/ec2/terraform/). Follow [`deploy/ec2/README.md`](./deploy/ec2/README.md) to provision the instance, point DNS at its IP, configure private environment variables, and run Docker Compose with Caddy TLS.

This is a single-instance deployment with MySQL stored on the EC2 Docker volume, not a highly available database service. For real customer data, use off-instance backups and consider Amazon RDS for MySQL, automated recovery, monitoring, and a managed secret store. The Terraform configuration provisions billable AWS resources only when you explicitly run `terraform apply`.

## Security and operational notes

- Spring Security uses BCrypt password hashes, session authentication, and CSRF protection for browser API requests.
- The public EC2 security group exposes only ports 80 and 443; administration uses Systems Manager instead of public SSH.
- Set `SESSION_COOKIE_SECURE=true` when serving behind HTTPS. The EC2 Compose override enables it.
- New users create their own accounts; the empty local database has no pre-created accounts.
- Password reset, administrator account management, persistent/email notifications, rate limiting, and multi-instance high availability are not implemented.
