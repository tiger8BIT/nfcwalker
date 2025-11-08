# Project Memory: NFC Walker Patrol System

## Quick Commands (IMPORTANT!)
**Always use Java 21 for gradle commands:**
```bash
cd /Users/vyacheslavkolodynskiy/IdeaProjects/nfcwalker && export JAVA_HOME=$(/usr/libexec/java_home -v 21) && ./gradlew [command]
```

## Current Status
- ✅ Project compiles successfully with Java 21
- ✅ All domain entities created (8 entities)
- ✅ All repositories created (8 repositories)
- ✅ Flyway migrations ready (V1__core.sql, V2__challenge_used.sql)
- ✅ Admin API controller implemented
- ✅ Scan API controller with challenge-response flow implemented
- ✅ ChallengeService with replay attack prevention
- ✅ AWS Lambda handler (LambdaHandler)
- ✅ GCP Functions handler (GcpHttpFunction)
- ⚠️ Tests fail - Docker/Testcontainers connection issue (not a code problem)

## Last Build Result
```
BUILD FAILED - Tests can't connect to PostgreSQL via Testcontainers
Compilation: SUCCESS ✅
Code quality: All errors fixed ✅
```

## Architecture Overview
- **Three entry points**: AWS Lambda, GCP Functions (2nd gen), Embedded Netty
- **Database**: PostgreSQL with Flyway migrations
- **Security**: HS256 JWS challenges with replay prevention
- **Pool**: Hikari max 3 connections (serverless-friendly)

## Domain Entities (JPA)
1. `Organization` - top-level org
2. `Site` - physical location
3. `Checkpoint` - scan point with optional geo (lat/lon/radius)
4. `PatrolRoute` - route definition
5. `PatrolRouteCheckpoint` - checkpoint in route with sequence + time windows
6. `PatrolRun` - scheduled patrol execution
7. `PatrolScanEvent` - recorded scan with verdict
8. `ChallengeUsed` - replay attack prevention (PK=jti)

## API Endpoints

### Admin API (`/api/admin`)
- `POST /checkpoints` - create checkpoint
- `GET /checkpoints?siteId=X` - list by site
- `POST /routes` - create route
- `POST /routes/{id}/points` - bulk add checkpoints to route

### Scan API (`/api/scan`)
- `POST /start` - returns challenge (JWS) + policy
- `POST /finish` - validates challenge, creates event, prevents replay (409 on duplicate)

## Key Dependencies
- Micronaut 4.6.1 / 4.10.1
- Kotlin 1.9.25
- Java 21
- Micronaut Data JPA + Hibernate
- Flyway
- Nimbus JOSE+JWT 9.37.3
- AWS Lambda support
- GCP Functions support (functions-framework-api 1.1.0)
- Testcontainers for tests
- SnakeYAML for application.yml

## Configuration
- `application.yml` - main config with env vars (JDBC_URL, JDBC_USER, JDBC_PASSWORD, APP_CHALLENGE_SECRET, JWT_SECRET)
- `application.properties` - OLD, should be ignored (application.yml takes precedence)

## Known Issues Fixed
1. ✅ Java 11 → 21 (must use export JAVA_HOME in terminal)
2. ✅ Data class → regular class for JPA entities
3. ✅ Conflicting constructors removed
4. ✅ LambdaHandler execute() override removed
5. ✅ SnakeYAML dependency added
6. ✅ Google Cloud Functions API dependency added

## Test Structure
- Uses `@MicronautTest` with embedded server + Kotest (StringSpec style)
- Testcontainers PostgreSQL (requires Docker running)
- Tests use JWT authentication with HS256 (min 256-bit secret required)
- Important: Kotest tests are NOT isolated by default (SingleInstance mode)
  - Each test case can reuse state from previous ones
  - For independent tests, use separate test classes or explicit setup
  - Extract common fixtures into base test class or @TestConfiguration bean
- Test issues fixed:
  - JWT secret must be 32 bytes+ (app.challenge.secret in config)
  - Authentication setup needed in base test config
  - Foreign key constraints require proper data setup order
  - Duplicate challenge prevention (409 Conflict on replay)
- Common test setup should be extracted to avoid duplication

## Deployment Entry Points
1. **AWS Lambda**: `ge.tiger8bit.LambdaHandler`
2. **GCP Functions**: `ge.tiger8bit.GcpHttpFunction`
3. **Local/Docker**: `ge.tiger8bit.ApplicationKt` (main)

## Build Commands
```bash
# Build without tests
cd /Users/vyacheslavkolodynskiy/IdeaProjects/nfcwalker && export JAVA_HOME=$(/usr/libexec/java_home -v 21) && ./gradlew build -x test

# Build with tests (requires Docker)
cd /Users/vyacheslavkolodynskiy/IdeaProjects/nfcwalker && export JAVA_HOME=$(/usr/libexec/java_home -v 21) && ./gradlew build

# Run locally
cd /Users/vyacheslavkolodynskiy/IdeaProjects/nfcwalker && export JAVA_HOME=$(/usr/libexec/java_home -v 21) && ./gradlew run
```

## Next Steps (if needed)
- [ ] Fix Docker/Testcontainers setup to run tests
- [ ] Add JWT authentication for Admin API
- [ ] Implement geo-fencing validation in scan finish
- [ ] Add time window validation
- [ ] Add route sequence validation

