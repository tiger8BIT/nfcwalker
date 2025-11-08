# Project Memory: NFC Walker Patrol System
_Last updated: 2025-11-09_

This file is for the assistant's fast recall. Keep it **concise**, **actionable**, and **current**. Avoid marketing fluff or duplication of the README. Update when architecture, invariants, or active tasks change.

---
## 1. Invariants (Stable Facts)
- Language / Runtime: Kotlin (1.9.25), Java 21 (mandatory)
- Framework: Micronaut 4.x (HTTP server + DI + security)
- Persistence: PostgreSQL + Hibernate (Micronaut Data JPA) + Flyway migrations (V1, V2 existing)
- **ID Strategy: UUID (all entities use `GenerationType.UUID` with `gen_random_uuid()` default in DB)**
- Security Model: HS256 challenge-response (JWS) + replay prevention via `challenge_used` (PK = jti)
- Deployment Entry Points:
  - Local / Netty: `ge.tiger8bit.ApplicationKt`
  - AWS Lambda: `ge.tiger8bit.LambdaHandler`
  - GCP Function (HTTP): `ge.tiger8bit.GcpHttpFunction`
- Domain Entities (8): Organization, Site, Checkpoint, PatrolRoute, PatrolRouteCheckpoint, PatrolRun, PatrolScanEvent, ChallengeUsed
- Primary DB connection pool small (Hikari) for serverless friendliness (max ~3)

## 2. Current State (Mutable Snapshot)
- Build: Compiles with Java 21
- Tests: Kotest-based integration tests; each creates unique data with UUID. Independent & can run in parallel.
  - Some integration tests depend on Docker / Testcontainers (PostgreSQL). If Docker down -> failures.
  - **Note: Terminal may hang on gradle commands; ensure Docker is running and ports are free.**
- Known runtime validation: HS256 secret must be ≥ 32 bytes (256 bits) or Nimbus throws `KeyLengthException`.
- Replay logic returns 409 (expected) but may currently surface 500 if not caught (verify error mapping).

## 3. Active / Pending Tasks
Use checkboxes; keep list short. Remove when done.
- [x] ~~Switch test isolation away from stateful ordering~~ – Each test creates unique data with UUID. Tests are now independent & safe to run in parallel (Kotest handles parallelism internally).
- [ ] Ensure duplicate challenge (replay) returns 409 not 500 – map `ConstraintViolationException` properly
- [ ] Add geo-fencing validation (lat/lon/radius) in finish scan flow
- [ ] Enforce route sequence & optional time windows

## 4. Risks / Gotchas
| Area | Issue | Mitigation |
|------|-------|-----------|
| Secrets | Too-short HS256 secret | Enforce length check at startup |
| Tests | Rely on ordered state (SingleInstance Kotest) | Refactor to explicit fixtures |
| Replay Handling | DB constraint surfaces 500 | Convert to 409 via exception handler |
| FK Errors | Missing Org/Site before creating Checkpoint | Provide seed util / fixture |
| AOP | Final methods block interceptors | Keep services `open` if advice needed |
| Code Quality | Copy-paste DTO mappings | Use extension functions on domain models |

## 5. Commands (Generic)

**Build & Test (with Java 21):**
```bash
# Build (skip tests)
export JAVA_HOME=$(/usr/libexec/java_home -v 21) && ./gradlew build -x test

# Full build with tests (needs Docker running)
export JAVA_HOME=$(/usr/libexec/java_home -v 21) && ./gradlew build

# Run tests only
export JAVA_HOME=$(/usr/libexec/java_home -v 21) && ./gradlew test

# Run specific test class
export JAVA_HOME=$(/usr/libexec/java_home -v 21) && ./gradlew test --tests "ge.tiger8bit.NfcwalkerTest"

# Run tests with detailed output
export JAVA_HOME=$(/usr/libexec/java_home -v 21) && ./gradlew test --info

# Clean build (nuke cache)
export JAVA_HOME=$(/usr/libexec/java_home -v 21) && ./gradlew clean build
```

**Server & Development (with Java 21):**
```bash
# Run local server on :8080
export JAVA_HOME=$(/usr/libexec/java_home -v 21) && ./gradlew run

# Build & run shadow JAR
export JAVA_HOME=$(/usr/libexec/java_home -v 21) && ./gradlew shadowJar
java -jar build/libs/nfcwalker-*.jar
```

**Troubleshooting:**
```bash
# If gradle hangs: ensure Docker is running & no port conflicts
ps aux | grep gradlew

# Force kill if needed
pkill -9 java
```

## 6. Configuration Keys (Env-driven)
| Key | Purpose | Notes |
|-----|---------|-------|
| JDBC_URL | PostgreSQL URL | Default local port 5432 |
| JDBC_USER / JDBC_PASSWORD | DB credentials | Use non-prod creds locally |
| APP_CHALLENGE_SECRET | HS256 signing for challenges | ≥ 32 bytes required |
| JWT_SECRET | (Future) auth / admin token signing | Keep same entropy policy |
| MICRONAUT_ENV | Environment profile | dev / test / prod |

## 7. Domain Cheat Sheet
- `ChallengeUsed.jti`: uniqueness = replay prevention
- `PatrolScanEvent`: attaches scan outcome; depends on valid challenge
- `PatrolRouteCheckpoint`: sequence + optional time windows (TODO validation)

## 8. Testing Strategy
- Style: Kotest StringSpec + `@MicronautTest`
- Isolation: **FIXED** – Each test creates completely unique data (Organization/Site/Checkpoint/Route) with UUID. No state carry-over. Tests are independent & safe to run in parallel.
- Needed Fixtures: `TestFixtures.seedOrgAndSite()` returns (Org, Site) pair; checkpoints created via API in each test
- Replay Test: Use same challenge twice → expect HTTP 409 (assert mapping)

## 9. Error Mapping TODO
Implement / confirm `@Error` or global exception handler to translate:
- `ConstraintViolationException` on duplicate JTI → 409 CONFLICT
- FK violation when missing parent → 400 BAD_REQUEST (custom message) instead of 500

## 10. Future Enhancements (Not Yet Started)
- External auth integration (Google-based identity / roles: admin vs worker)
- Geo-fence evaluation (distance calc) on finish
- Patrol route adherence scoring / missed checkpoints
- Observability: structured logging + tracing ids

## 11. Non-Goals (For Now)
- Multi-tenant sharding
- Real-time websocket push
- Complex RBAC beyond admin/worker prototype

## 12. Update Procedure
When making a significant change:
1. Adjust Invariants (section 1) if architecture shifts
2. Refresh Current State + Active Tasks
3. Prune stale tasks / risks
4. Keep this file < ~250 lines

## 13. Quick Validation Checklist (Pre-Deploy)
- [ ] All migrations applied & repeatable scripts clean
- [ ] Challenge secret length OK
- [ ] Replay duplicate returns 409
- [ ] Health endpoint (if any) responds 200
- [ ] Build with tests green under Docker

---
## 14. Recent Changes Log (Short Rolling Window)
| Date       | Change                                                                                                                                                                                                                                                                                                                |
|------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 2025-11-09 | **Test isolation fixed** – Each test creates unique Organization/Site/Checkpoint with UUID. Fixed PatrolRouteCheckpoint creation to pass IDs to constructor (not apply block) to avoid Hibernate NonUniqueObjectException with zero UUID defaults. Removed unnecessary Gradle parallel config that was causing hangs. |
| 2025-11-09 | **Comprehensive logging added** – All app logic covered with SLF4J: DEBUG for development, INFO for business events, WARN for exceptions. Replay attacks, validations, and business operations tracked.                                                                                                               |
| 2025-11-09 | **All IDs converted to UUID** – All 8 entities use `GenerationType.UUID`; DB migrations V1 & V2 updated; ready for distributed deployments.                                                                                                                                                                           |
| 2025-11-08 | **Code refactoring completed** – AdminController: extracted mapping functions (DRY). ScanController: extracted helpers (buildScanPolicy, parseChallenge, findActivePatrolRun).                                                                                                                                        |
| 2025-11-09 | **Kotest parallel config fixed** – Correct types (Int?) for concurrentSpecs/concurrentTests with @OptIn(ExperimentalKotest). Added note to avoid Boolean misuse (was causing red highlight).                                                                                                                          |

---
### Usage Notes (For Assistant)
- Prefer this file for context; avoid scraping full code unless necessary.
- Do not echo absolute local paths back to user unless asked.
- When user asks "what next?" use Active Tasks + Risks to propose actions.
