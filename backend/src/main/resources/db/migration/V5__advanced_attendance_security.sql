ALTER TABLE sites ADD COLUMN geofence_radius_m INT NULL;
ALTER TABLE sites ADD COLUMN require_geofence BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE sites ADD COLUMN require_registered_device BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE sites ADD COLUMN require_dynamic_qr BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE registered_devices (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    identifier_hash CHAR(64) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    platform VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by BIGINT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    approved_at TIMESTAMP(6) NULL,
    last_used_at TIMESTAMP(6) NULL,
    CONSTRAINT fk_device_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_device_approver FOREIGN KEY (approved_by) REFERENCES users(id),
    CONSTRAINT uk_device_employee_hash UNIQUE (employee_id, identifier_hash)
);

CREATE TABLE dynamic_qr_challenges (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    site_id BIGINT NOT NULL,
    code_hash CHAR(64) NOT NULL UNIQUE,
    valid_from TIMESTAMP(6) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_qr_site FOREIGN KEY (site_id) REFERENCES sites(id),
    CONSTRAINT fk_qr_creator FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX ix_qr_site_validity (site_id, active, expires_at)
);

CREATE TABLE punch_attempts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    accepted BOOLEAN NOT NULL,
    reason_code VARCHAR(50) NOT NULL,
    ip_address VARCHAR(64),
    device_hash CHAR(64),
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    channel VARCHAR(30),
    correlation_id VARCHAR(64),
    CONSTRAINT fk_attempt_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    INDEX ix_attempt_employee_time (employee_id, occurred_at),
    INDEX ix_attempt_result_time (accepted, occurred_at)
);
