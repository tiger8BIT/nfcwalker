# NFC Walker

Patrol tracking system built on NFC. Admins set up routes, workers scan checkpoints, the system records everything (GPS, photos, sub-checks, incidents).

**Stack**: Kotlin, Micronaut 4, PostgreSQL, JWT, Docker. Deploys to local / AWS Lambda / GCP Cloud Functions.

## How it works

There are three roles: **APP_OWNER** (manages organizations), **BOSS** (manages sites/routes/workers), **WORKER** (executes patrols).

Data flows top-down: **Organization** > **Site** > **Checkpoint** > **Route** > **Patrol Run** > **Scan Events**.

Scanning is a two-phase challenge-response flow:

1. Worker taps NFC tag and calls `POST /api/scan/start` with the checkpoint code.
2. Server returns a short-lived JWT **challenge** and a **scan policy** (GPS rules, required photos, sub-checks).
3. Worker collects evidence and calls `POST /api/scan/finish` with the challenge + results.
4. Server validates everything and returns a **verdict**: `OK`, `WARNING` (problems found but scan valid), or `FAIL` (validation failed).

Incidents can be reported during a scan or standalone, and are managed by bosses.

## Run locally

**Requires**: Java 21, Docker, Docker Compose.

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew clean shadowJar -Plocal
# first time: edit .env.docker and replace placeholder secrets
docker-compose up -d
curl http://localhost:8080/health
```

See [docs/RUNNING_LOCALLY.md](docs/RUNNING_LOCALLY.md) for the full guide (env vars, dev endpoints, troubleshooting).

## Tests

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test
```

There is also a Postman collection with full end-to-end flows (Owner > Boss > Worker): [docs/nfcwalker.postman_collection.json](docs/nfcwalker.postman_collection.json).

## Docs

| What | Where |
|------|-------|
| API spec (Swagger UI, live) | [tiger8bit.github.io/nfcwalker](https://tiger8bit.github.io/nfcwalker/) |
| API spec (OpenAPI, auto-generated) | [docs/openapi.yml](docs/openapi.yml) |
| Local dev guide | [docs/RUNNING_LOCALLY.md](docs/RUNNING_LOCALLY.md) |
| User flow diagrams | [docs/diagrams/USER_FLOWS.md](docs/diagrams/USER_FLOWS.md) |
| Patrol lifecycle state machine | [docs/diagrams/PATROL_LIFECYCLE.md](docs/diagrams/PATROL_LIFECYCLE.md) |
| Domain model (ER diagram) | [docs/diagrams/DOMAIN_MODEL.md](docs/diagrams/DOMAIN_MODEL.md) |
| Postman collection | [docs/nfcwalker.postman_collection.json](docs/nfcwalker.postman_collection.json) |

## License

Proprietary. All rights reserved.
