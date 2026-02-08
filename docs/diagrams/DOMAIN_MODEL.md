# Domain Model (Entity-Relationship Diagram)

This diagram illustrates the core entities and their relationships in the NFC Walker system. Essential for understanding data structure, API
design, and frontend state management.

```mermaid
erDiagram
    ORGANIZATION ||--o{ SITE : "contains"
    ORGANIZATION ||--o{ USER_ROLE : "assigns"
    ORGANIZATION ||--o{ DEVICE : "manages"
    ORGANIZATION ||--o{ INVITATION : "creates"
    ORGANIZATION ||--o{ INCIDENT : "tracks"
    
    USER ||--o{ USER_ROLE : "has"
    USER ||--o{ DEVICE : "owns"
    USER ||--o{ PATROL_RUN : "executes"
    
    SITE ||--o{ CHECKPOINT : "contains"
    SITE ||--o{ PATROL_ROUTE : "defines"
    
    CHECKPOINT ||--o{ CHECKPOINT_SUB_CHECK : "requires"
    CHECKPOINT ||--o{ PATROL_ROUTE_CHECKPOINT : "used in"
    
    PATROL_ROUTE ||--o{ PATROL_ROUTE_CHECKPOINT : "includes (ordered)"
    PATROL_ROUTE ||--o{ PATROL_RUN : "executes as"
    
    PATROL_RUN ||--o{ PATROL_SCAN_EVENT : "records"
    PATROL_SCAN_EVENT ||--o{ PATROL_SUB_CHECK_EVENT : "validates"
    PATROL_SCAN_EVENT ||--o{ ATTACHMENT : "includes photos"
    
    INCIDENT ||--o{ ATTACHMENT : "includes photos"
    INCIDENT }o--|| USER : "reported by"
    
    ORGANIZATION {
        uuid id PK
        string name
        timestamp created_at
        timestamp updated_at
    }
    
    USER {
        uuid id PK
        string email UK
        string password_hash
        timestamp created_at
    }
    
    USER_ROLE {
        uuid id PK
        uuid user_id FK
        uuid organization_id FK
        enum role "ROLE_APP_OWNER, ROLE_BOSS, ROLE_WORKER"
        timestamp created_at
    }
    
    SITE {
        uuid id PK
        uuid organization_id FK
        string name
        string address
        decimal geo_lat
        decimal geo_lon
        timestamp created_at
    }
    
    CHECKPOINT {
        uuid id PK
        uuid site_id FK
        uuid organization_id FK
        string code UK "e.g., CP-101"
        string label "e.g., Main Entrance"
        decimal geo_lat
        decimal geo_lon
        int radius_m "GPS tolerance"
        timestamp created_at
    }
    
    CHECKPOINT_SUB_CHECK {
        uuid id PK
        uuid checkpoint_id FK
        string label "e.g., Check Fire Extinguisher"
        boolean is_mandatory
        timestamp created_at
    }
    
    PATROL_ROUTE {
        uuid id PK
        uuid site_id FK
        uuid organization_id FK
        string name
        timestamp created_at
    }
    
    PATROL_ROUTE_CHECKPOINT {
        uuid id PK
        uuid route_id FK
        uuid checkpoint_id FK
        int seq "Sequence order"
        int min_offset_sec "Earliest scan time"
        int max_offset_sec "Latest scan time"
    }
    
    PATROL_RUN {
        uuid id PK
        uuid route_id FK
        uuid user_id FK "Worker"
        enum status "PENDING, IN_PROGRESS, COMPLETED, CANCELLED, MISSED"
        timestamp scheduled_at
        timestamp started_at
        timestamp ended_at
        timestamp created_at
    }
    
    PATROL_SCAN_EVENT {
        uuid id PK
        uuid patrol_run_id FK
        uuid checkpoint_id FK
        uuid device_id FK
        decimal scanned_lat
        decimal scanned_lon
        timestamp scanned_at
        enum verdict "OK, FAIL"
        timestamp created_at
    }
    
    PATROL_SUB_CHECK_EVENT {
        uuid id PK
        uuid scan_event_id FK
        uuid sub_check_id FK
        boolean result
        string note
        timestamp created_at
    }
    
    DEVICE {
        uuid id PK
        uuid user_id FK
        uuid organization_id FK
        string device_id UK "External device identifier"
        json metadata "Platform, model, etc."
        enum status "ACTIVE, INACTIVE, BLOCKED"
        timestamp registered_at
        timestamp last_used_at
    }
    
    INCIDENT {
        uuid id PK
        uuid organization_id FK
        uuid reporter_id FK "User who reported"
        uuid patrol_run_id FK "Optional"
        string description
        enum status "OPEN, IN_PROGRESS, RESOLVED"
        enum severity "LOW, MEDIUM, HIGH, CRITICAL"
        timestamp reported_at
        timestamp resolved_at
    }
    
    ATTACHMENT {
        uuid id PK
        uuid scan_event_id FK "Optional: linked to scan"
        uuid incident_id FK "Optional: linked to incident"
        string storage_key "S3/GCS key"
        string mime_type
        int size_bytes
        timestamp created_at
    }
    
    INVITATION {
        uuid id PK
        uuid organization_id FK
        string email
        enum role "ROLE_BOSS, ROLE_WORKER"
        string token UK
        enum status "PENDING, ACCEPTED, EXPIRED"
        timestamp created_at
        timestamp expires_at
    }
```

## Entity Descriptions

### Core Entities

- **ORGANIZATION**: Top-level tenant. Contains sites, users, and all data.
- **USER**: System user. Can have multiple roles across organizations.
- **USER_ROLE**: Links user to organization with specific role (APP_OWNER, BOSS, WORKER).
- **SITE**: Physical location within organization. Contains checkpoints and routes.

### Infrastructure

- **CHECKPOINT**: Physical scan point with NFC tag. Has GPS coordinates and tolerance radius.
- **CHECKPOINT_SUB_CHECK**: Additional tasks required at checkpoint (e.g., "Check fire alarm").
- **PATROL_ROUTE**: Ordered sequence of checkpoints defining a patrol path.
- **PATROL_ROUTE_CHECKPOINT**: Junction table linking checkpoints to routes with sequence and timing.

### Operations

- **PATROL_RUN**: Instance of a route being executed by a worker. Tracks status (PENDING → IN_PROGRESS → COMPLETED).
- **PATROL_SCAN_EVENT**: Record of worker scanning a checkpoint. Includes GPS, timestamp, and verdict.
- **PATROL_SUB_CHECK_EVENT**: Result of completing a sub-check at a checkpoint.
- **DEVICE**: Worker's mobile device registered for patrol operations.

### Incidents & Media

- **INCIDENT**: Issue reported by worker. Can include photos and be linked to patrol run.
- **ATTACHMENT**: Photo/file uploaded by worker. Can be linked to scan event or incident.
- **INVITATION**: Pending invitation for user to join organization with specific role.

## Key Relationships

1. **Multi-tenancy**: All data is scoped to ORGANIZATION
2. **Role-based access**: USER_ROLE defines permissions per organization
3. **Spatial validation**: CHECKPOINT has GPS coordinates; PATROL_SCAN_EVENT validates worker location
4. **Ordered sequences**: PATROL_ROUTE_CHECKPOINT.seq defines checkpoint order
5. **Challenge-response**: PATROL_SCAN_EVENT links to DEVICE for security validation
6. **Flexible media**: ATTACHMENT can link to either scan events or incidents

## Frontend State Management Guidelines

For Redux/Zustand implementations:

- **Normalize entities** by ID for efficient lookups
- **Cache patrol runs** by status (PENDING, IN_PROGRESS)
- **Group checkpoints** by site for route visualization
- **Track scan progress** as percentage (scanned / total checkpoints)
- **Real-time updates** via polling or WebSocket for active patrol runs
