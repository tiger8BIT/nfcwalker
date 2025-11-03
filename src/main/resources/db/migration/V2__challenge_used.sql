-- Challenge tracking to prevent replay attacks

CREATE TABLE challenge_used (
    jti VARCHAR(100) PRIMARY KEY,
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NOT NULL DEFAULT NOW(),
    device_id VARCHAR(200) NOT NULL,
    checkpoint_id BIGINT NOT NULL,
    CONSTRAINT fk_challenge_used_checkpoint FOREIGN KEY (checkpoint_id) REFERENCES checkpoints(id)
);

CREATE INDEX idx_challenge_used_expires_at ON challenge_used(expires_at);

