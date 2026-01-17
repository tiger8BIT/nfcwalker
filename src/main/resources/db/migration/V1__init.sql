-- Initial schema for NFC Patrol System
-- Combined from previous V1-V7 migrations

-- 0. Enums
CREATE TYPE user_role AS ENUM ('ROLE_APP_OWNER', 'ROLE_BOSS', 'ROLE_WORKER');
CREATE TYPE device_status AS ENUM ('ACTIVE', 'INACTIVE', 'BLOCKED');
CREATE TYPE invitation_status AS ENUM ('PENDING', 'ACCEPTED', 'EXPIRED', 'REVOKED');
CREATE TYPE patrol_run_status AS ENUM ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'MISSED');
CREATE TYPE scan_verdict AS ENUM ('OK', 'WARNING', 'FAIL');
CREATE TYPE check_status AS ENUM ('OK', 'PROBLEMS_FOUND', 'SKIPPED');
CREATE TYPE incident_severity AS ENUM ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL');
CREATE TYPE incident_status AS ENUM ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED');
CREATE TYPE attachment_entity_type AS ENUM ('checkpoint', 'scan_event', 'sub_check_event', 'incident');

-- 1. Authentication & User Management
CREATE TABLE users
(
    id         UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    email      VARCHAR(255) NOT NULL UNIQUE,
    google_id  VARCHAR(255) UNIQUE,
    name       VARCHAR(200) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_google_id ON users (google_id);

CREATE TABLE organizations
(
    id         UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    name       VARCHAR(200) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE user_roles
(
    user_id         UUID        NOT NULL,
    organization_id UUID        NOT NULL,
    role user_role NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, organization_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_organization FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_roles_user_id ON user_roles (user_id);
CREATE INDEX idx_user_roles_organization_id ON user_roles (organization_id);

CREATE TABLE devices
(
    id              UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL,
    organization_id UUID         NOT NULL,
    device_id       VARCHAR(255) NOT NULL,
    metadata        JSONB,
    status device_status NOT NULL DEFAULT 'ACTIVE',
    registered_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_used_at    TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_devices_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_devices_organization FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE,
    UNIQUE (organization_id, device_id)
);

CREATE INDEX idx_devices_user_id ON devices (user_id);
CREATE INDEX idx_devices_organization_id ON devices (organization_id);
CREATE INDEX idx_devices_device_id ON devices (device_id);

CREATE TABLE invitations
(
    id              UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL,
    organization_id UUID         NOT NULL,
    role   user_role         NOT NULL,
    token           VARCHAR(255) NOT NULL UNIQUE,
    status invitation_status NOT NULL DEFAULT 'PENDING',
    created_by      UUID,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMP    NOT NULL,
    accepted_at     TIMESTAMP,
    CONSTRAINT fk_invitations_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_invitations_organization FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE
);

CREATE INDEX idx_invitations_email ON invitations (email);
CREATE INDEX idx_invitations_organization_id ON invitations (organization_id);
CREATE INDEX idx_invitations_token ON invitations (token);
CREATE INDEX idx_invitations_status ON invitations (status);

-- 2. Organizations & Sites
CREATE TABLE sites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_sites_organization FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

CREATE INDEX idx_sites_organization_id ON sites(organization_id);

CREATE TABLE checkpoints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    site_id UUID NOT NULL,
    code VARCHAR(100) NOT NULL UNIQUE,
    label         VARCHAR(200),
    geo_lat NUMERIC(10, 7),
    geo_lon NUMERIC(10, 7),
    radius_m NUMERIC(6, 2),
    require_photo BOOLEAN DEFAULT FALSE,
    allow_notes   BOOLEAN DEFAULT TRUE,
    description   VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_checkpoints_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_checkpoints_site FOREIGN KEY (site_id) REFERENCES sites(id)
);

CREATE INDEX idx_checkpoints_organization_id ON checkpoints(organization_id);
CREATE INDEX idx_checkpoints_site_id ON checkpoints(site_id);
CREATE INDEX idx_checkpoints_code ON checkpoints(code);

CREATE TABLE checkpoint_sub_checks
(
    id            UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    checkpoint_id UUID         NOT NULL,
    label         VARCHAR(200) NOT NULL,
    description   TEXT,
    require_photo BOOLEAN               DEFAULT FALSE,
    allow_notes   BOOLEAN               DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_csc_checkpoint FOREIGN KEY (checkpoint_id) REFERENCES checkpoints (id) ON DELETE CASCADE
);

CREATE INDEX idx_checkpoint_sub_checks_checkpoint_id ON checkpoint_sub_checks (checkpoint_id);

CREATE TABLE patrol_routes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    site_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_patrol_routes_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_patrol_routes_site FOREIGN KEY (site_id) REFERENCES sites(id)
);

CREATE INDEX idx_patrol_routes_organization_id ON patrol_routes(organization_id);
CREATE INDEX idx_patrol_routes_site_id ON patrol_routes(site_id);

CREATE TABLE patrol_route_checkpoints (
    route_id UUID NOT NULL,
    checkpoint_id UUID NOT NULL,
    seq INT NOT NULL,
    min_offset_sec INT NOT NULL DEFAULT 0,
    max_offset_sec INT NOT NULL DEFAULT 3600,
    PRIMARY KEY (route_id, checkpoint_id),
    CONSTRAINT fk_prc_route FOREIGN KEY (route_id) REFERENCES patrol_routes(id),
    CONSTRAINT fk_prc_checkpoint FOREIGN KEY (checkpoint_id) REFERENCES checkpoints(id)
);

CREATE INDEX idx_prc_route_id ON patrol_route_checkpoints(route_id);

CREATE TABLE patrol_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    route_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    planned_start TIMESTAMP NOT NULL,
    planned_end TIMESTAMP NOT NULL,
    status patrol_run_status NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_patrol_runs_route FOREIGN KEY (route_id) REFERENCES patrol_routes(id),
    CONSTRAINT fk_patrol_runs_organization FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

CREATE INDEX idx_patrol_runs_route_id ON patrol_runs(route_id);
CREATE INDEX idx_patrol_runs_organization_id ON patrol_runs(organization_id);
CREATE INDEX idx_patrol_runs_status ON patrol_runs(status);

CREATE TABLE patrol_scan_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patrol_run_id UUID NOT NULL,
    checkpoint_id UUID NOT NULL,
    user_id UUID NOT NULL,
    scanned_at TIMESTAMP NOT NULL,
    lat NUMERIC(10, 7),
    lon NUMERIC(10, 7),
    verdict      scan_verdict NOT NULL DEFAULT 'OK',
    check_status check_status,
    check_notes  TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_pse_patrol_run FOREIGN KEY (patrol_run_id) REFERENCES patrol_runs(id),
    CONSTRAINT fk_pse_checkpoint FOREIGN KEY (checkpoint_id) REFERENCES checkpoints(id)
);

CREATE INDEX idx_pse_patrol_run_id ON patrol_scan_events(patrol_run_id);
CREATE INDEX idx_pse_checkpoint_id ON patrol_scan_events(checkpoint_id);
CREATE INDEX idx_pse_user_id ON patrol_scan_events(user_id);
CREATE INDEX idx_pse_scanned_at ON patrol_scan_events(scanned_at);

CREATE TABLE patrol_sub_check_events
(
    id            UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    scan_event_id UUID         NOT NULL,
    sub_check_id  UUID         NOT NULL,
    status        check_status NOT NULL,
    notes         TEXT,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_psce_scan_event FOREIGN KEY (scan_event_id) REFERENCES patrol_scan_events (id) ON DELETE CASCADE,
    CONSTRAINT fk_psce_sub_check FOREIGN KEY (sub_check_id) REFERENCES checkpoint_sub_checks (id) ON DELETE CASCADE
);

CREATE INDEX idx_psce_scan_event_id ON patrol_sub_check_events (scan_event_id);

-- 3. Challenge Tracking
CREATE TABLE challenge_used
(
    jti           VARCHAR(100) PRIMARY KEY,
    issued_at     TIMESTAMP    NOT NULL,
    expires_at    TIMESTAMP    NOT NULL,
    used_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    device_id     VARCHAR(200) NOT NULL,
    checkpoint_id UUID         NOT NULL,
    CONSTRAINT fk_challenge_used_checkpoint FOREIGN KEY (checkpoint_id) REFERENCES checkpoints (id)
);

CREATE INDEX idx_challenge_used_expires_at ON challenge_used (expires_at);

-- 4. Incidents & Attachments
CREATE TABLE incidents
(
    id              UUID PRIMARY KEY           DEFAULT gen_random_uuid(),
    organization_id UUID              NOT NULL,
    site_id         UUID              NOT NULL,
    checkpoint_id   UUID,
    scan_event_id   UUID,
    reported_by     UUID              NOT NULL,
    description     TEXT              NOT NULL,
    severity        incident_severity NOT NULL DEFAULT 'MEDIUM',
    status          incident_status   NOT NULL DEFAULT 'OPEN',
    created_at      TIMESTAMP         NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP         NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_incidents_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_incidents_site FOREIGN KEY (site_id) REFERENCES sites (id),
    CONSTRAINT fk_incidents_checkpoint FOREIGN KEY (checkpoint_id) REFERENCES checkpoints (id),
    CONSTRAINT fk_incidents_scan_event FOREIGN KEY (scan_event_id) REFERENCES patrol_scan_events (id),
    CONSTRAINT fk_incidents_reported_by FOREIGN KEY (reported_by) REFERENCES users (id)
);

CREATE INDEX idx_incidents_organization_id ON incidents (organization_id);
CREATE INDEX idx_incidents_site_id ON incidents (site_id);
CREATE INDEX idx_incidents_status ON incidents (status);

CREATE TABLE attachments
(
    id            UUID PRIMARY KEY                DEFAULT gen_random_uuid(),
    entity_type   attachment_entity_type NOT NULL,
    entity_id     UUID                   NOT NULL,
    file_path     VARCHAR(512)           NOT NULL,
    original_name VARCHAR(255),
    content_type  VARCHAR(100),
    file_size     BIGINT,
    created_at    TIMESTAMP              NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_attachments_entity ON attachments (entity_type, entity_id);
