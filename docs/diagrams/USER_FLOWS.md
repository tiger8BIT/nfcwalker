# System User Flows

This diagram shows how different roles interact with the NFC Walker API across the complete lifecycle: from organization setup through
worker patrol execution.

```mermaid
sequenceDiagram
    autonumber
    actor Admin as BOSS / APP_OWNER
    actor Worker as WORKER
    participant API as NFC Walker API
    participant DB as PostgreSQL

    Note over Admin, API: 1. Infrastructure Setup
    Admin->>API: POST /api/organizations
    API-->>Admin: orgId
    Admin->>API: POST /api/sites (orgId)
    API-->>Admin: siteId
    Admin->>API: POST /api/admin/checkpoints (siteId, code, GPS)
    API-->>Admin: checkpointId, code, subCheckIds
    Admin->>API: POST /api/admin/routes (siteId, name)
    API-->>Admin: routeId
    Admin->>API: POST /api/admin/routes/{id}/points
    Note right of API: Links checkpoints to route with sequence
    Admin->>API: POST /api/patrol-runs (routeId, orgId)
    API-->>Admin: patrolRunId, status=IN_PROGRESS

    Note over Worker, API: 2. Worker Onboarding
    Admin->>API: POST /api/invitations (email, role=WORKER)
    API-->>Admin: invitationId
    Worker->>API: POST /auth/invite/accept (token)
    API-->>Worker: JWT with WORKER role
    Worker->>API: POST /api/devices (deviceId, metadata)
    API->>DB: Register device
    API-->>Worker: deviceId

    Note over Worker, API: 3. Patrol Scanning Flow
    Worker->>API: POST /api/scan/start (orgId, deviceId, code)
    Note right of API: Validates checkpoint code & active patrol run
    API-->>Worker: Challenge JWT + ScanPolicy (GPS/Photo rules)
    Worker->>Worker: Perform GPS validation
    Worker->>Worker: Capture required photos
    Worker->>Worker: Complete sub-checks
    Worker->>API: POST /api/scan/finish (Challenge, Photos, SubChecks)
    API->>DB: Verify challenge & save scan events
    API->>DB: Store attachments (photos)
    API-->>Worker: Verdict (OK/WARNING/FAIL) + eventId

    Note over Worker, Admin: 4. Incident Reporting & Management
    Worker->>API: POST /api/incidents (description, photos, metadata)
    API->>DB: Save incident
    API-->>Worker: incidentId
    Admin->>API: GET /api/incidents (organizationId)
    API-->>Admin: List of incidents
    Admin->>API: PATCH /api/incidents/{id} (status: RESOLVED)
    API->>DB: Update incident status
    API-->>Admin: Updated incident
```

## API Flow Details

### 1. Infrastructure Setup (Admin)

- Creates hierarchical structure: Organization → Site → Checkpoint → Route
- Each checkpoint has unique code (e.g., "CP-101") for NFC scanning
- Routes link multiple checkpoints in sequence with timing constraints
- Creates patrol run instance (status: IN_PROGRESS by default)

### 2. Worker Onboarding

- Admin invites worker via email
- Worker accepts invitation and receives JWT token with WORKER role
- Worker registers device(s) for patrol operations

### 3. Patrol Scanning

- Two-phase scan: `start` (get challenge) → `finish` (submit results)
- Challenge JWT prevents replay attacks and ensures scan freshness
- ScanPolicy defines GPS tolerance, photo requirements, sub-checks
- System validates GPS coordinates, photos, and sub-check completion
- Returns verdict: OK (no issues), WARNING (problems found or incidents), FAIL (validation failed)

### 4. Incident Management

- Workers report issues during patrol (embedded in finish scan) or standalone
- Admins review and resolve incidents
- Full audit trail maintained in database
- Photos can be attached to incidents
