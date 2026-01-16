-- Migration to add incidents
CREATE TABLE incidents
(
    id              UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    organization_id UUID        NOT NULL,
    site_id         UUID        NOT NULL,
    checkpoint_id   UUID,
    scan_event_id   UUID,
    reported_by     UUID        NOT NULL,
    description     TEXT        NOT NULL,
    severity        VARCHAR(50) NOT NULL,                -- 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'
    status          VARCHAR(50) NOT NULL DEFAULT 'OPEN', -- 'OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_incidents_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_incidents_site FOREIGN KEY (site_id) REFERENCES sites (id),
    CONSTRAINT fk_incidents_checkpoint FOREIGN KEY (checkpoint_id) REFERENCES checkpoints (id),
    CONSTRAINT fk_incidents_scan_event FOREIGN KEY (scan_event_id) REFERENCES patrol_scan_events (id),
    CONSTRAINT fk_incidents_reported_by FOREIGN KEY (reported_by) REFERENCES users (id)
);

CREATE INDEX idx_incidents_organization_id ON incidents (organization_id);
CREATE INDEX idx_incidents_site_id ON incidents (site_id);
CREATE INDEX idx_incidents_status ON incidents (status);
