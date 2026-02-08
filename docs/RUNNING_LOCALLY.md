# 🚀 Running NFC Walker Locally

This guide explains how to run the NFC Walker API locally using Docker Compose.

## Prerequisites

- **Docker** and **Docker Compose** installed
- **Java 21** (for building the application)
- **Postman** (optional, for testing)

## Quick Start

### 1. Clone and Build

```bash
git clone <repository-url>
cd nfcwalker

# Build the application (this also sets up .env.docker automatically)
./gradlew clean shadowJar -Plocal
```

**What happens during build:**

- Gradle automatically copies `.env.docker.example` → `.env.docker` (if it doesn't exist)
- The `.env.docker` file is created with placeholder values
- **⚠️ Important**: You must replace placeholders before running (see next step)

### 2. Configure Environment

Edit `.env.docker` and replace placeholders:

```bash
# Open the file
nano .env.docker

# Replace these REQUIRED values:
JWT_SECRET=<REPLACE_WITH_SECURE_32_CHAR_STRING>
APP_CHALLENGE_SECRET=<REPLACE_WITH_SECURE_32_CHAR_STRING>
```

**Quick setup for local development:**

```bash
# Generate secure secrets automatically
sed -i.bak "s/<REPLACE_WITH_SECURE_32_CHAR_STRING>/$(openssl rand -base64 32)/g" .env.docker

# Or use these pre-filled dev values (NOT for production!)
JWT_SECRET=dev-jwt-secret-minimum-32-chars-required-for-hs256-algorithm
APP_CHALLENGE_SECRET=dev-challenge-secret-minimum-32-chars-required-secure
```

**Optional OAuth/SMTP** (can be left as placeholders for local dev):

- `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` - for OAuth login
- `SMTP_USERNAME` / `SMTP_PASSWORD` - for email invitations

### 2. Configure Environment

Edit `.env.docker` and replace placeholders:

```bash
# Open the file
nano .env.docker

# Replace these REQUIRED values:
JWT_SECRET=<REPLACE_WITH_SECURE_32_CHAR_STRING>
APP_CHALLENGE_SECRET=<REPLACE_WITH_SECURE_32_CHAR_STRING>
```

**Quick setup for local development:**

```bash
# Generate secure secrets automatically
sed -i.bak "s/<REPLACE_WITH_SECURE_32_CHAR_STRING>/$(openssl rand -base64 32)/g" .env.docker

# Or use these pre-filled dev values (NOT for production!)
JWT_SECRET=dev-jwt-secret-minimum-32-chars-required-for-hs256-algorithm
APP_CHALLENGE_SECRET=dev-challenge-secret-minimum-32-chars-required-secure
```

**Optional OAuth/SMTP** (can be left as placeholders for local dev):

- `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` - for OAuth login
- `SMTP_USERNAME` / `SMTP_PASSWORD` - for email invitations

### 3. Start Services

```bash
docker-compose up -d
```

This starts:

- **PostgreSQL** (port 5432) – Database
- **NFC Walker API** (port 8080) – Backend service

### 4. Verify Services

```bash
# Check if services are running
docker-compose ps

# View application logs
docker-compose logs app --tail 50

# Check API health
curl http://localhost:8080/health
```

### 5. Test with Postman

1. Import `docs/nfcwalker.postman_collection.json` into Postman
2. Run the entire collection (includes auto-cleanup)
3. Or run individual folders/requests as needed

## Environment Configuration

### Understanding .env.docker

The project uses `.env.docker` for local environment variables:

1. **Automatic creation**: On first build, Gradle copies `.env.docker.example` → `.env.docker`
2. **Contains placeholders**: You must replace `<REPLACE_WITH_*>` values before running
3. **Git-friendly**: Local changes won't be committed (via `skip-worktree`)

**Required placeholders to replace:**

```properties
# MUST REPLACE THESE:
JWT_SECRET=<REPLACE_WITH_SECURE_32_CHAR_STRING>
APP_CHALLENGE_SECRET=<REPLACE_WITH_SECURE_32_CHAR_STRING>
# OPTIONAL (can leave as placeholders for local dev):
GOOGLE_CLIENT_ID=<REPLACE_WITH_GOOGLE_CLIENT_ID>
GOOGLE_CLIENT_SECRET=<REPLACE_WITH_GOOGLE_CLIENT_SECRET>
SMTP_USERNAME=<REPLACE_WITH_SMTP_USERNAME>
SMTP_PASSWORD=<REPLACE_WITH_SMTP_PASSWORD>
```

**Pre-configured values (no changes needed):**

```properties
# Database (matches docker-compose.yml)
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_DB=nfcwalker
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
# Application
MICRONAUT_ENVIRONMENTS=local
# Email (MailHog for local testing - optional)
SMTP_HOST=mailhog
SMTP_PORT=1025
```

### Quick Setup Options

**Option 1: Auto-generate secure secrets (recommended)**

```bash
# Replace all placeholders with random values
sed -i.bak "s/<REPLACE_WITH_SECURE_32_CHAR_STRING>/$(openssl rand -base64 32)/g" .env.docker
```

**Option 2: Use dev-friendly values**

```bash
# Manually edit .env.docker and set:
JWT_SECRET=dev-jwt-secret-minimum-32-chars-required-for-hs256-algorithm
APP_CHALLENGE_SECRET=dev-challenge-secret-minimum-32-chars-required-secure

# Optional: Leave OAuth/SMTP as placeholders (local dev will work without them)
```

**Option 3: Manual generation**

```bash
# Generate secrets one by one
openssl rand -base64 32  # Use for JWT_SECRET
openssl rand -base64 32  # Use for APP_CHALLENGE_SECRET
```

### Production Configuration

For production deployment, use environment-specific `.env` files with proper secrets:

```bash
# Production secrets (NEVER commit these)
JWT_SECRET=$(openssl rand -base64 32)
APP_CHALLENGE_SECRET=$(openssl rand -base64 32)
```

## Useful Commands

### Application

```bash
# Rebuild and restart application
./gradlew clean shadowJar -Plocal
docker-compose up -d --build app

# View application logs
docker-compose logs app -f

# Stop application
docker-compose stop app

# Restart application
docker-compose restart app
```

### Database

```bash
# Reset database (delete all data)
docker-compose down
docker volume rm nfcwalker_postgres-data
docker-compose up -d

# Connect to PostgreSQL
docker-compose exec postgres psql -U postgres -d nfcwalker

# Backup database
docker-compose exec postgres pg_dump -U postgres nfcwalker > backup.sql

# Restore database
docker-compose exec -T postgres psql -U postgres nfcwalker < backup.sql
```

### Cleanup

```bash
# Stop all services
docker-compose down

# Remove all data (including database)
docker-compose down -v

# Remove Docker images
docker-compose down --rmi all
```

## Development Endpoints

The following endpoints are **only available in local environment** (`MICRONAUT_ENVIRONMENTS=local`):

### Dev Authentication

```bash
# Magic login (no password required)
POST /auth/dev/login?email=owner@nfcwalker.com
# Returns JWT token with ROLE_APP_OWNER
```

### Dev Database

```bash
# Reset database (delete all test data)
DELETE /api/dev/database/reset
# Preserves System Root organization
```

### Dev Invitations

```bash
# Get invitation token for testing
GET /api/dev/invitations/{invitationId}/token
# Returns: {"token": "abc123..."}
```

## Troubleshooting

### Application won't start

```bash
# Check logs
docker-compose logs app --tail 100

# Common issues:
# - PostgreSQL not ready: Wait 10-15 seconds and restart app
# - Port 8080 already in use: Stop other services or change port
# - Build failed: Run ./gradlew clean shadowJar -Plocal again
```

### "JWT_SECRET" or "APP_CHALLENGE_SECRET" errors

**Error message:** `Required property JWT_SECRET not set` or similar

**Cause:** You haven't replaced placeholders in `.env.docker`

**Fix:**

```bash
# Quick auto-fix
sed -i.bak "s/<REPLACE_WITH_SECURE_32_CHAR_STRING>/$(openssl rand -base64 32)/g" .env.docker
docker-compose restart app

# Or manually edit .env.docker and replace:
# JWT_SECRET=<REPLACE_WITH_SECURE_32_CHAR_STRING>
# APP_CHALLENGE_SECRET=<REPLACE_WITH_SECURE_32_CHAR_STRING>
```

### Database connection errors

```bash
# Check PostgreSQL is running
docker-compose ps postgres

# Restart PostgreSQL
docker-compose restart postgres

# Check connection from app container
docker-compose exec app nc -zv postgres 5432
```

### Postman collection fails

```bash
# Reset database first
curl -X DELETE http://localhost:8080/api/dev/database/reset

# Or run the full collection (includes reset as first request)
```

## Architecture

### Service Dependencies

```
┌─────────────────┐
│  Postman        │
│  (Testing)      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  NFC Walker API │
│  (Port 8080)    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  PostgreSQL     │
│  (Port 5432)    │
└─────────────────┘
```

### Project Structure

```
nfcwalker/
├── src/main/kotlin/ge/tiger8bit/
│   ├── controller/        # REST endpoints
│   ├── service/          # Business logic
│   ├── repository/       # Data access
│   ├── domain/          # JPA entities
│   └── dto/             # Data transfer objects
├── src/main/resources/
│   ├── application.yml  # Micronaut config
│   ├── application-local.yml  # Local overrides
│   └── db/migration/    # Flyway SQL migrations
├── docs/
│   ├── nfcwalker.postman_collection.json
│   └── openapi.yml      # API specification
├── docker-compose.yml   # Local environment
├── .env.docker         # Local configuration
└── build.gradle.kts    # Build configuration
```

## Next Steps

- 📖 Read the [API Documentation](https://tiger8bit.github.io/nfcwalker/)
- 🔍 Explore [User Flow Diagrams](docs/diagrams/USER_FLOWS.md)
- 🧪 Run the [Postman Collection](docs/nfcwalker.postman_collection.json)
- 🏗️ Review the [Domain Model](docs/diagrams/DOMAIN_MODEL.md)

## Production Deployment

For production deployment:

1. Use proper secrets management (AWS Secrets Manager, HashiCorp Vault, etc.)
2. Enable SSL/TLS
3. Configure OAuth providers
4. Set up monitoring and logging
5. Use production-grade database (RDS, Cloud SQL, etc.)
6. Deploy as serverless (Lambda/Cloud Functions) or containerized (ECS/GKE)

See deployment-specific guides in `docs/deployment/` (to be added).
