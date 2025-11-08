# NFC Walker - Patrol System

A serverless-first, multi-platform patrol tracking system using NFC checkpoints. Built with **Kotlin**, **Micronaut**, and **PostgreSQL**.

## Quick Start

### Prerequisites
- **JDK 21** (required!)
- **Gradle** (included via wrapper)
- **Docker** (for Testcontainers - if running tests)

### Build & Run

**Always set Java 21 in your terminal before running gradle commands:**

```bash
cd /Users/vyacheslavkolodynskiy/IdeaProjects/nfcwalker
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# Build (skip tests)
./gradlew build -x test

# Build with tests (requires Docker)
./gradlew build

# Run locally
./gradlew run

# Run specific test
./gradlew test --tests NfcwalkerTest
```

## Architecture

### Three Deployment Targets
1. **AWS Lambda** → `ge.tiger8bit.LambdaHandler`
2. **GCP Cloud Functions (2nd gen)** → `ge.tiger8bit.GcpHttpFunction`
3. **Embedded Netty Server** → `ge.tiger8bit.ApplicationKt` (local/Docker)

### Database & Migrations
- **PostgreSQL** backend
- **Flyway** migrations (V1__core.sql, V2__challenge_used.sql)
- **Connection pool**: Hikari, max 3 (serverless-optimized)

### Security Model
- **Challenge-Response Flow**: HS256 JWS (HMAC-SHA256)
- **Replay Attack Prevention**: Unique JTI tracking in `challenge_used` table
- **Authentication**: JWT Bearer tokens (signed challenges)

## Domain Model

```
Organization (top-level container)
  └── Site (physical location)
      ├── Checkpoint (NFC scan point with optional geo-fencing)
      └── PatrolRoute (defined route with sequence)
          └── PatrolRouteCheckpoint (checkpoint in route + time windows)

PatrolRun (scheduled execution instance)
  └── PatrolScanEvent (recorded scan + verdict)

ChallengeUsed (replay attack prevention - PK=jti)
```

## API Endpoints

### Admin API (`/api/admin`)
Protected endpoints for organization administrators.

**Create Checkpoint:**
```http
POST /api/admin/checkpoints
Content-Type: application/json
Authorization: Bearer <jwt>

{
  "code": "CP-001",
  "siteId": 1,
  "geoLat": 41.7151,
  "geoLon": 44.7671,
  "radiusM": 50
}
```

**List Checkpoints by Site:**
```http
GET /api/admin/checkpoints?siteId=1
Authorization: Bearer <jwt>
```

**Create Patrol Route:**
```http
POST /api/admin/routes
Content-Type: application/json
Authorization: Bearer <jwt>

{
  "name": "Morning Route",
  "organizationId": 1
}
```

**Add Checkpoints to Route:**
```http
POST /api/admin/routes/{routeId}/points
Content-Type: application/json
Authorization: Bearer <jwt>

[
  {
    "checkpointId": 1,
    "sequence": 1,
    "windowStartMinutes": 0,
    "windowDurationMinutes": 30
  },
  ...
]
```

### Scan API (`/api/scan`)
Public endpoints for field workers.

**Start Scan (Request Challenge):**
```http
POST /api/scan/start
Content-Type: application/json

{
  "checkpointId": 1,
  "deviceId": "device-uuid-123"
}
```

**Response:**
```json
{
  "challenge": "eyJhbGc...",
  "policy": {
    "issueTime": "2025-11-08T10:30:00Z",
    "expiryTime": "2025-11-08T10:35:00Z",
    "jti": "60d31fbe-cb11-4404-89f1-7de00b579da5"
  }
}
```

**Finish Scan (Submit Challenge + Proof):**
```http
POST /api/scan/finish
Content-Type: application/json

{
  "checkpointId": 1,
  "deviceId": "device-uuid-123",
  "signedProof": "eyJhbGc...",
  "payload": {
    "jti": "60d31fbe-cb11-4404-89f1-7de00b579da5",
    "timestamp": "2025-11-08T10:31:00Z"
  }
}
```

**Responses:**
- `200 OK` - Scan recorded successfully
- `409 Conflict` - Challenge already used (replay attack detected)
- `401 Unauthorized` - Invalid/expired challenge
- `400 Bad Request` - Missing checkpoint or invalid data

## Configuration

### Environment Variables
Set in your deployment environment or in `application.yml`:

```yaml
app:
  challenge:
    secret: "your-32-byte-min-secret-here" # MUST be 32+ bytes for HS256

jwt:
  secret: "your-jwt-secret-32-bytes-min" # For token signing

datasources:
  default:
    url: ${JDBC_URL:jdbc:postgresql://localhost:5432/nfcwalker}
    username: ${JDBC_USER:postgres}
    password: ${JDBC_PASSWORD:postgres}
    schema-generate: create_drop # Use 'create_drop' for tests, 'none' for production
    dialect: POSTGRES
```

### Running with Docker Compose
```bash
# Start PostgreSQL
docker-compose up -d

# Run the app
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./gradlew run
```

**docker-compose.yml example:**
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: nfcwalker
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
```

## Testing

### Test Architecture
- Framework: **Kotest** (StringSpec style) + **Micronaut Test**
- Database: **Testcontainers PostgreSQL** (Docker required)
- Authentication: JWT with HS256 signatures
- Isolation: ⚠️ Kotest tests use `SingleInstance` mode (state shared between tests)

### Important Test Notes

**1. Kotest Isolation**
By default, Kotest StringSpec runs all tests in a single instance. This means:
- ✅ Good for integration tests that build on previous state
- ❌ Bad for unit tests expecting isolation
- **Solution**: Extract common setup into base test class or use `@TestConfiguration`

**2. JWT Secret Size**
The challenge service requires a minimum 256-bit (32-byte) secret:
```kotlin
// ❌ FAILS
val secret = "short-secret"

// ✅ OK
val secret = "a-very-long-secret-that-is-at-least-32-bytes-long"
```

**3. Test Database Setup**
Tests automatically:
- Create PostgreSQL container via Testcontainers
- Run Flyway migrations
- Populate test data
- Rollback after each test (if using `@Transactional`)

**4. Running Tests**
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# All tests
./gradlew test

# Single test class
./gradlew test --tests NfcwalkerTest

# With verbose output
./gradlew test --info
```

## Kotlin + Spring AOP Note

Kotlin classes and methods are **final by default**, which prevents Spring AOP from applying advice.

### Solution: Enable Kotlin All-Open Plugin

**In `build.gradle.kts`:**
```kotlin
plugins {
    kotlin("plugin.spring") version "1.9.25"
}

allOpen {
    annotation("io.micronaut.aop.Interceptable")
    annotation("jakarta.inject.Singleton")
    annotation("io.micronaut.transaction.annotation.Transactional")
}
```

This makes classes/methods decorated with these annotations `open` for proxying.

### Or Manually Open
```kotlin
open class MyService {
    open fun advicedMethod() { }
}
```

## Known Issues & Workarounds

| Issue | Status | Fix |
|-------|--------|-----|
| Java version mismatch | ✅ Fixed | Use `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` |
| JWT secret too short | ✅ Fixed | Ensure secret is 32+ bytes in config |
| Foreign key constraint fails | ✅ Fixed | Seed data in correct order in tests |
| Testcontainers connection timeout | ✅ Fixed | Ensure Docker is running; rebuild images |
| AOP advice not applied | ✅ Mitigated | Apply `kotlin-spring` plugin |

## Project Structure

```
src/
├── main/
│   ├── kotlin/ge/tiger8bit/
│   │   ├── ApplicationKt              (main entry point)
│   │   ├── LambdaHandler.kt           (AWS Lambda handler)
│   │   ├── GcpHttpFunction.kt         (GCP Functions handler)
│   │   ├── controller/                (REST endpoints)
│   │   ├── domain/                    (JPA entities)
│   │   ├── repository/                (Data repositories)
│   │   └── service/                   (Business logic)
│   └── resources/
│       ├── application.yml            (main config)
│       └── db/migration/              (Flyway migrations)
└── test/
    ├── kotlin/ge/tiger8bit/
    │   └── NfcwalkerTest.kt          (integration tests)
    └── resources/
        └── application-test.yml       (test config)
```

## Build Artifacts

After `./gradlew build`:
- **JAR**: `build/libs/nfcwalker-0.1.jar` (regular)
- **Shadow JAR**: `build/libs/nfcwalker-0.1-shadow.jar` (with deps)
- **Native Image**: `build/libs/nfcwalker-native` (GraalVM, if enabled)
- **Lambda ZIP**: `build/distributions/nfcwalker-0.1.zip`

## Deployment

### Local Docker
```bash
docker build -t nfcwalker .
docker run -e JDBC_URL=jdbc:postgresql://host.docker.internal:5432/nfcwalker \
           -e JDBC_USER=postgres \
           -e JDBC_PASSWORD=postgres \
           -p 8080:8080 nfcwalker
```

### AWS Lambda
```bash
./gradlew build -x test
# Upload build/distributions/nfcwalker-0.1.zip to AWS Lambda
# Set Handler: ge.tiger8bit.LambdaHandler
```

### GCP Cloud Functions (2nd Gen)
```bash
# Deploy from build/libs/nfcwalker-0.1-all.jar
# Set Entry Point: ge.tiger8bit.GcpHttpFunction
gcloud functions deploy nfcwalker \
  --gen2 \
  --runtime java21 \
  --trigger-http \
  --allow-unauthenticated \
  --source .
```

## Contributing

### Code Style
- Kotlin, 4-space indents
- `open` classes/methods for AOP-advised code
- Data classes for DTOs, regular classes for JPA entities
- Document public APIs with KDoc

### Before Committing
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./gradlew build -x test  # Ensure compilation passes
./gradlew ktlintFormat   # Auto-format (if ktlint enabled)
```

## License
Proprietary – Tiger 8 Bit

## Contact
Project maintained by Tiger 8 Bit development team.

