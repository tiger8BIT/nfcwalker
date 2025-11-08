# NFC Walker

NFC patrol tracking service built with Kotlin, Micronaut, and PostgreSQL. Runs locally (Netty) or serverless (AWS Lambda, GCP Cloud Functions).

## Prerequisites
- Java 21
- Gradle wrapper
- Docker (for Testcontainers-based tests)
- PostgreSQL 15+ (if running locally without Docker)

## Quick start

```bash
# macOS: set Java 21 for all Gradle commands
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# Build (skip tests)
./gradlew clean build -x test

# Run locally (expects a PostgreSQL database)
./gradlew run

# Run tests (requires Docker running)
./gradlew test
```

## Configuration (env vars)
Set via your shell or environment; secrets must be at least 32 bytes (256 bits) for HS256.

```bash
# Database
export JDBC_URL=jdbc:postgresql://localhost:5432/nfcwalker
export JDBC_USER=postgres
export JDBC_PASSWORD=postgres

# Security
export APP_CHALLENGE_SECRET="change-me-32-bytes-minimum"  # required for challenge JWS
export JWT_SECRET="change-me-32-bytes-minimum"            # if JWT tokens are used
```

## Entry points
- Local main: `ge.tiger8bit.ApplicationKt`
- AWS Lambda handler: `ge.tiger8bit.LambdaHandler`
- GCP Cloud Functions entry: `ge.tiger8bit.GcpHttpFunction`

## Build artifacts
- After `./gradlew build`, jars are in `build/libs/`

## License
Proprietary © Tiger 8 Bit. All rights reserved.
