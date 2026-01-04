-- Migration to add sub-checks to Checkpoints
CREATE TABLE checkpoint_sub_checks
(
    id            UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    checkpoint_id UUID         NOT NULL,
    label         VARCHAR(200) NOT NULL,
    description   TEXT,
    require_photo BOOLEAN               DEFAULT FALSE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_csc_checkpoint FOREIGN KEY (checkpoint_id) REFERENCES checkpoints (id) ON DELETE CASCADE
);

CREATE INDEX idx_checkpoint_sub_checks_checkpoint_id ON checkpoint_sub_checks (checkpoint_id);

-- Table for recording results of sub-checks
CREATE TABLE patrol_sub_check_events
(
    id            UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    scan_event_id UUID        NOT NULL,
    sub_check_id  UUID        NOT NULL,
    status        VARCHAR(50) NOT NULL, -- 'ok', 'problems_found'
    notes         TEXT,
    created_at    TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_psce_scan_event FOREIGN KEY (scan_event_id) REFERENCES patrol_scan_events (id) ON DELETE CASCADE,
    CONSTRAINT fk_psce_sub_check FOREIGN KEY (sub_check_id) REFERENCES checkpoint_sub_checks (id) ON DELETE CASCADE
);

CREATE INDEX idx_psce_scan_event_id ON patrol_sub_check_events (scan_event_id);
