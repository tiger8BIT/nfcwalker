-- Migration to expand Checkpoint and PatrolScanEvent
-- Added label and configuration for checkpoints
ALTER TABLE checkpoints
    ADD COLUMN label VARCHAR(200);
ALTER TABLE checkpoints
    ADD COLUMN require_photo BOOLEAN DEFAULT FALSE;
ALTER TABLE checkpoints
    ADD COLUMN allow_notes BOOLEAN DEFAULT TRUE;
ALTER TABLE checkpoints
    ADD COLUMN custom_instructions VARCHAR(500);

-- Added check results to patrol_scan_events
ALTER TABLE patrol_scan_events
    ADD COLUMN check_status VARCHAR(50); -- 'ok', 'problems_found'
ALTER TABLE patrol_scan_events
    ADD COLUMN check_notes TEXT;

-- Table for attachments (photos)
CREATE TABLE attachments
(
    id            UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    entity_type   VARCHAR(50)  NOT NULL, -- 'checkpoint', 'scan_event'
    entity_id     UUID         NOT NULL,
    file_path     VARCHAR(512) NOT NULL,
    original_name VARCHAR(255),
    content_type  VARCHAR(100),
    file_size     BIGINT,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_attachments_entity ON attachments (entity_type, entity_id);
