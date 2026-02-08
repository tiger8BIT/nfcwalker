# NFC Walker

NFC-based patrol tracking and security monitoring system.

## Quick Start

```bash
# Build the application
./gradlew clean shadowJar -Plocal

# Start services
docker-compose up -d

# Verify
curl http://localhost:8080/health
```

**First time setup:** Edit `.env.docker` and replace placeholder secrets before starting.

Quick fix:
```bash
sed -i.bak "s/<REPLACE_WITH_SECURE_32_CHAR_STRING>/$(openssl rand -base64 32)/g" .env.docker
docker-compose restart app
```

## Core Features

**Multi-tenant architecture**

- Organizations → Sites → Checkpoints → Routes hierarchy
- Role-based access (APP_OWNER, BOSS, WORKER)

**NFC checkpoint scanning**

- Challenge-response security (prevents replay attacks)
- GPS validation with configurable radius
- Photo verification and sub-checks
- Real-time verdict (OK/WARNING/FAIL)

**Patrol management**

- Create patrol runs from routes
- Track status (PENDING → IN_PROGRESS → COMPLETED)
- Time windows for checkpoint scanning
- Automatic status transitions

**Incident reporting**

- Workers report issues during patrol or standalone
- Photo attachments and severity levels
- Link incidents to scan events
- Boss-level incident management

**Device management**

- Register worker devices
- Track device status and usage
- Per-organization device isolation

## System Architecture

**Tech stack:**

- Kotlin + Micronaut 4.x
- PostgreSQL + Flyway migrations
- JWT authentication (HS256)
- Reactive streams (Reactor)
- Docker / AWS Lambda / GCP Cloud Functions support

**Domain hierarchy:**

```
Organization
└── Site
    ├── Checkpoint (+ SubChecks)
    └── Route
        └── PatrolRun (+ ScanEvents)
```

**API Endpoints:**

- `/api/organizations` - Organization management
- `/api/sites` - Site management
- `/api/admin/checkpoints` - Checkpoint CRUD
- `/api/admin/routes` - Route configuration
- `/api/patrol-runs` - Create patrol runs
- `/api/scan/start` - Issue scan challenge
- `/api/scan/finish` - Submit scan results
- `/api/incidents` - Incident management
- `/api/devices` - Device registration
- `/api/invitations` - User invitations
- `/auth/*` - Authentication endpoints

## Key Workflows

### 1. Infrastructure Setup (BOSS/APP_OWNER)

```
Create Organization → Create Site → Create Checkpoints → Create Route → Add Checkpoints to Route
```

### 2. Worker Onboarding (BOSS invites WORKER)

```
Send Invitation → Worker Accepts → Worker Registers Device
```

### 3. Patrol Execution (WORKER)

```
Boss Creates Patrol Run → Worker Scans Checkpoint (start) → Worker Gets Challenge + Policy → 
Worker Submits Results (finish) → System Records Scan Event → Return Verdict
```

### 4. Incident Management

```
Worker Reports Incident (during scan or standalone) → Boss Reviews Incidents → Boss Updates Status
```

## Security

**Challenge-response flow:**

1. Worker calls `/api/scan/start` with checkpoint code
2. System issues JWT challenge (valid 5 min, single-use)
3. Worker submits results with challenge via `/api/scan/finish`
4. System validates challenge, prevents replay, records event

**JWT tokens:**

- HS256 with 32-byte secret minimum
- Contains user ID, organization roles
- Refresh not implemented (long-lived tokens in dev)

**Replay protection:**

- Challenge JTI stored in `challenge_used` table
- Duplicate challenge submission returns HTTP 409

## Data Model Key Points

**Enums:**

- `user_role`: ROLE_APP_OWNER, ROLE_BOSS, ROLE_WORKER
- `patrol_run_status`: PENDING, IN_PROGRESS, COMPLETED, CANCELLED, MISSED
- `scan_verdict`: OK, WARNING, FAIL
- `check_status`: OK, PROBLEMS_FOUND, SKIPPED
- `incident_status`: OPEN, IN_PROGRESS, RESOLVED, CLOSED
- `incident_severity`: LOW, MEDIUM, HIGH, CRITICAL

**Important tables:**

- `checkpoints` - Physical scan points with GPS coordinates
- `checkpoint_sub_checks` - Additional tasks at checkpoints
- `patrol_routes` - Named sequences of checkpoints
- `patrol_route_checkpoints` - Junction table with timing constraints
- `patrol_runs` - Active patrol instances
- `patrol_scan_events` - Individual scan records
- `patrol_sub_check_events` - Sub-check completion records
- `incidents` - Issues reported by workers
- `attachments` - Photos linked to scans, sub-checks, or incidents
- `challenge_used` - Replay protection tracking

## Testing

**Run tests:**

```bash
./gradlew test
```

**Postman collection:**

- Idempotent (auto-resets database)
- Three complete flows: Owner → Boss → Worker
- Import `docs/nfcwalker.postman_collection.json`

**Test coverage:**

- Organization/Site/Checkpoint/Route CRUD
- Invitation flows
- Scan flows (start/finish)
- Sub-checks and photos
- Incident management
- Replay attack prevention

## Local Development

**Prerequisites:**

- Java 21
- Docker & Docker Compose

**Setup:**

1. Clone repo
2. Run `./gradlew clean shadowJar -Plocal` (auto-creates `.env.docker`)
3. Edit `.env.docker` - replace `<REPLACE_WITH_*>` placeholders
4. Run `docker-compose up -d`

**Dev endpoints (local env only):**

- `POST /auth/dev/login?email=owner@nfcwalker.com` - Magic login
- `DELETE /api/dev/database/reset` - Reset DB
- `GET /api/dev/invitations/{id}/token` - Get invitation token

**Useful commands:**

```bash
# Rebuild and restart
./gradlew clean shadowJar -Plocal && docker-compose up -d --build app

# View logs
docker-compose logs app -f

# Reset database
docker-compose down -v && docker-compose up -d

# Connect to PostgreSQL
docker-compose exec postgres psql -U postgres -d nfcwalker
```

## Documentation

- **[API Specification](https://tiger8bit.github.io/nfcwalker/)** - OpenAPI 3.0.1
- **[Local Setup Guide](docs/RUNNING_LOCALLY.md)** - Detailed local dev instructions
- **[User Flows](docs/diagrams/USER_FLOWS.md)** - Sequence diagrams for key flows
- **[Patrol Lifecycle](docs/diagrams/PATROL_LIFECYCLE.md)** - State machine diagram
- **[Domain Model](docs/diagrams/DOMAIN_MODEL.md)** - Entity relationships
- **[Postman Collection](docs/nfcwalker.postman_collection.json)** - API tests

## Deployment

**Build profiles:**

```bash
# Local (Netty)
./gradlew shadowJar -Plocal

# AWS Lambda
./gradlew shadowJar -Plambda

# GCP Cloud Functions
./gradlew shadowJar -Pgcf
```

**Entry points:**

- Local: `ge.tiger8bit.ApplicationKt`
- AWS Lambda: `ge.tiger8bit.LambdaHandler`
- GCP: `ge.tiger8bit.GcpHttpFunction`

**Required environment variables:**
```bash
# Security (minimum 32 bytes)
JWT_SECRET=<secure-random-string>
APP_CHALLENGE_SECRET=<secure-random-string>

# Database
JDBC_URL=jdbc:postgresql://host:5432/nfcwalker
JDBC_USER=postgres
JDBC_PASSWORD=<secure-password>

# Optional: OAuth
OAUTH_GOOGLE_CLIENT_ID=<google-client-id>
OAUTH_GOOGLE_CLIENT_SECRET=<google-client-secret>

# Optional: Email
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=<email>
SMTP_PASSWORD=<password>
```

## Project Structure

```
nfcwalker/
├── src/main/kotlin/ge/tiger8bit/
│   ├── controller/          # REST endpoints
│   ├── service/             # Business logic
│   ├── repository/          # Micronaut Data repositories
│   ├── domain/              # JPA entities
│   └── dto/                 # Request/response DTOs
├── src/main/resources/
│   ├── application.yml              # Base config
│   ├── application-local.yml        # Local overrides
│   └── db/migration/                # Flyway SQL
├── src/test/kotlin/ge/tiger8bit/spec/  # Integration tests (Kotest)
├── docs/
│   ├── diagrams/                    # Mermaid diagrams
│   ├── nfcwalker.postman_collection.json
│   └── openapi.yml                  # Generated API spec
├── docker-compose.yml
├── .env.docker                      # Local secrets (gitignored)
└── build.gradle.kts
```

## License

Proprietary © Tiger 8 Bit. All rights reserved.
