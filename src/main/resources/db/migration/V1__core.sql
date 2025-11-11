-- Core schema for NFC Patrol System
-- All IDs use UUID for better distributed system support

-- Authentication & User Management
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

CREATE TABLE user_roles
(
    user_id         UUID        NOT NULL,
    organization_id UUID        NOT NULL,
    role            VARCHAR(50) NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, organization_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
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
    status          VARCHAR(50)  NOT NULL DEFAULT 'active',
    registered_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_used_at    TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_devices_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
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
    role            VARCHAR(50)  NOT NULL,
    token           VARCHAR(255) NOT NULL UNIQUE,
    status          VARCHAR(50)  NOT NULL DEFAULT 'pending',
    created_by      UUID,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMP    NOT NULL,
    accepted_at     TIMESTAMP,
    CONSTRAINT fk_invitations_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_invitations_email ON invitations (email);
CREATE INDEX idx_invitations_organization_id ON invitations (organization_id);
CREATE INDEX idx_invitations_token ON invitations (token);
CREATE INDEX idx_invitations_status ON invitations (status);

-- Organizations & Sites
CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Add foreign key constraint for user_roles after organizations table exists
ALTER TABLE user_roles
    ADD CONSTRAINT fk_user_roles_organization FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE;
ALTER TABLE devices
    ADD CONSTRAINT fk_devices_organization FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE;
ALTER TABLE invitations
    ADD CONSTRAINT fk_invitations_organization FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE;

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
    geo_lat NUMERIC(10, 7),
    geo_lon NUMERIC(10, 7),
    radius_m NUMERIC(6, 2),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_checkpoints_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_checkpoints_site FOREIGN KEY (site_id) REFERENCES sites(id)
);

CREATE INDEX idx_checkpoints_organization_id ON checkpoints(organization_id);
CREATE INDEX idx_checkpoints_site_id ON checkpoints(site_id);
CREATE INDEX idx_checkpoints_code ON checkpoints(code);

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
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
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
    user_id VARCHAR(200) NOT NULL,
    scanned_at TIMESTAMP NOT NULL,
    lat NUMERIC(10, 7),
    lon NUMERIC(10, 7),
    verdict VARCHAR(50) NOT NULL DEFAULT 'ok',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_pse_patrol_run FOREIGN KEY (patrol_run_id) REFERENCES patrol_runs(id),
    CONSTRAINT fk_pse_checkpoint FOREIGN KEY (checkpoint_id) REFERENCES checkpoints(id)
);

CREATE INDEX idx_pse_patrol_run_id ON patrol_scan_events(patrol_run_id);
CREATE INDEX idx_pse_checkpoint_id ON patrol_scan_events(checkpoint_id);
CREATE INDEX idx_pse_user_id ON patrol_scan_events(user_id);
CREATE INDEX idx_pse_scanned_at ON patrol_scan_events(scanned_at);

-- End of core schema
