CREATE TABLE incidents (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    attendance_day_id BIGINT NULL,
    incident_type VARCHAR(40) NOT NULL,
    description VARCHAR(500) NOT NULL,
    status VARCHAR(30) NOT NULL,
    source VARCHAR(30) NOT NULL,
    occurred_on DATE NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_incident_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT fk_incident_attendance FOREIGN KEY (attendance_day_id) REFERENCES attendance_days (id),
    INDEX ix_incident_employee_date (employee_id, occurred_on)
);

CREATE TABLE employee_requests (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    incident_id BIGINT NULL,
    attendance_day_id BIGINT NULL,
    punch_id BIGINT NULL,
    request_type VARCHAR(30) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    requested_from TIMESTAMP(6) NULL,
    requested_to TIMESTAMP(6) NULL,
    requested_change VARCHAR(1000) NULL,
    evidence_reference VARCHAR(500) NULL,
    status VARCHAR(30) NOT NULL,
    reviewed_by BIGINT NULL,
    review_comment VARCHAR(1000) NULL,
    submitted_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    resolved_at TIMESTAMP(6) NULL,
    CONSTRAINT fk_request_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT fk_request_incident FOREIGN KEY (incident_id) REFERENCES incidents (id),
    CONSTRAINT fk_request_attendance FOREIGN KEY (attendance_day_id) REFERENCES attendance_days (id),
    CONSTRAINT fk_request_punch FOREIGN KEY (punch_id) REFERENCES punches (id),
    CONSTRAINT fk_request_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (id),
    INDEX ix_request_employee_status (employee_id, status),
    INDEX ix_request_status_date (status, submitted_at)
);

CREATE TABLE audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    occurred_at TIMESTAMP(6) NOT NULL,
    username VARCHAR(120),
    action VARCHAR(20) NOT NULL,
    resource VARCHAR(500) NOT NULL,
    result_status INT NOT NULL,
    ip_address VARCHAR(64),
    duration_ms BIGINT NOT NULL,
    correlation_id VARCHAR(64) NOT NULL,
    INDEX ix_audit_time (occurred_at),
    INDEX ix_audit_user_time (username, occurred_at)
);
