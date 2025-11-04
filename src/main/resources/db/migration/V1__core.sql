-- Core schema for NFC Patrol System

CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE sites (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_sites_organization FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

CREATE INDEX idx_sites_organization_id ON sites(organization_id);

CREATE TABLE checkpoints (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    site_id BIGINT NOT NULL,
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
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    site_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_patrol_routes_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_patrol_routes_site FOREIGN KEY (site_id) REFERENCES sites(id)
);

CREATE INDEX idx_patrol_routes_organization_id ON patrol_routes(organization_id);
CREATE INDEX idx_patrol_routes_site_id ON patrol_routes(site_id);

CREATE TABLE patrol_route_checkpoints (
    route_id BIGINT NOT NULL,
    checkpoint_id BIGINT NOT NULL,
    seq INT NOT NULL,
    min_offset_sec INT NOT NULL DEFAULT 0,
    max_offset_sec INT NOT NULL DEFAULT 3600,
    PRIMARY KEY (route_id, checkpoint_id),
    CONSTRAINT fk_prc_route FOREIGN KEY (route_id) REFERENCES patrol_routes(id),
    CONSTRAINT fk_prc_checkpoint FOREIGN KEY (checkpoint_id) REFERENCES checkpoints(id)
);

CREATE INDEX idx_prc_route_id ON patrol_route_checkpoints(route_id);

CREATE TABLE patrol_runs (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
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
    id BIGSERIAL PRIMARY KEY,
    patrol_run_id BIGINT NOT NULL,
    checkpoint_id BIGINT NOT NULL,
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
