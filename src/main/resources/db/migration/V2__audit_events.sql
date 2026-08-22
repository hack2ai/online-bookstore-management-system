CREATE TABLE audit_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_type VARCHAR(64) NOT NULL,
    user_id BIGINT NULL,
    resource_type VARCHAR(64) NULL,
    resource_id BIGINT NULL,
    request_id VARCHAR(128) NULL,
    details TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_audit_events_created_at (created_at),
    INDEX idx_audit_events_user_id (user_id),
    INDEX idx_audit_events_event_type (event_type),
    INDEX idx_audit_events_resource (resource_type, resource_id),
    CONSTRAINT fk_audit_events_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL
);
