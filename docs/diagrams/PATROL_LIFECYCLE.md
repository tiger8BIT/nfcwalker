# Patrol Run Lifecycle

This state machine diagram shows how patrol runs transition through different states during their lifecycle. This is essential for
understanding patrol status in the UI and business logic.

```mermaid
stateDiagram-v2
    [*] --> PENDING: Route scheduled by admin
    
    PENDING --> IN_PROGRESS: Worker scans first checkpoint
    PENDING --> CANCELLED: Admin cancels before start
    PENDING --> MISSED: Time window expires without scans
    
    IN_PROGRESS --> IN_PROGRESS: Worker scans next checkpoint
    IN_PROGRESS --> COMPLETED: All required checkpoints scanned
    IN_PROGRESS --> CANCELLED: Admin interrupts patrol
    IN_PROGRESS --> MISSED: Worker abandons patrol (timeout)
    
    COMPLETED --> [*]: Successful patrol completion
    CANCELLED --> [*]: Manual termination
    MISSED --> [*]: Automatic failure
    
    note right of PENDING
        Initial state when route is scheduled.
        Waiting for worker to start patrol.
    end note
    
    note right of IN_PROGRESS
        Active patrol in progress.
        Most checkpoints are scanned in this state.
        Real-time GPS and photo validation.
    end note
    
    note right of COMPLETED
        Success state: all checkpoints scanned.
        Reports generated, incidents resolved.
    end note
    
    note right of CANCELLED
        Manual termination by admin.
        Can occur at any stage.
    end note
    
    note right of MISSED
        Automatic failure due to timeout.
        System marks as missed if idle too long.
    end note
```

## State Descriptions

### PENDING

- **Entry**: Route is created and scheduled by admin
- **Purpose**: Waiting for worker to begin patrol
- **Exits to**:
    - `IN_PROGRESS`: Worker scans first checkpoint
    - `CANCELLED`: Admin cancels scheduled patrol
    - `MISSED`: Time window passes without any scans

### IN_PROGRESS

- **Entry**: Worker scans first checkpoint of the route
- **Purpose**: Active patrol execution with real-time scanning
- **Exits to**:
    - `IN_PROGRESS`: Worker continues scanning next checkpoints (loop)
    - `COMPLETED`: All required checkpoints successfully scanned
    - `CANCELLED`: Admin manually stops the patrol
    - `MISSED`: Worker fails to complete within time constraints

### COMPLETED

- **Entry**: All required checkpoints scanned successfully
- **Purpose**: Terminal success state
- **Business logic**:
    - Patrol report is generated
    - Performance metrics calculated
    - Incidents linked to this patrol are reviewed

### CANCELLED

- **Entry**: Admin manually terminates the patrol
- **Purpose**: Terminal state for interrupted patrols
- **Business logic**:
    - Partial scan data is preserved
    - Reason for cancellation may be recorded
    - Can occur from any non-terminal state

### MISSED

- **Entry**: System automatically marks patrol as failed
- **Purpose**: Terminal failure state due to timeout or abandonment
- **Business logic**:
    - No scans recorded within expected time window (PENDING → MISSED)
    - Patrol started but not completed in time (IN_PROGRESS → MISSED)
    - Alert may be sent to admin

## Implementation Notes

For frontend developers:

- **Poll status** for active patrols (IN_PROGRESS)
- **Show progress bar** based on checkpoints scanned vs. total
- **Display warnings** when approaching timeout (transition to MISSED)
- **Color coding**: Green (COMPLETED), Red (MISSED), Yellow (CANCELLED), Blue (IN_PROGRESS)
