ALTER TABLE punches
    ADD CONSTRAINT uk_punch_day_type UNIQUE (attendance_day_id, punch_type);

ALTER TABLE punch_attempts ADD COLUMN site_id BIGINT NULL;
ALTER TABLE punch_attempts ADD COLUMN punch_type VARCHAR(20) NULL;
ALTER TABLE punch_attempts ADD CONSTRAINT fk_attempt_site FOREIGN KEY (site_id) REFERENCES sites(id);

ALTER TABLE employee_requests ADD COLUMN affected_date DATE NULL;
ALTER TABLE employee_requests ADD COLUMN proposed_punch_type VARCHAR(20) NULL;
ALTER TABLE employee_requests ADD COLUMN proposed_at TIMESTAMP(6) NULL;

CREATE TABLE attendance_adjustments (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    attendance_day_id BIGINT NOT NULL,
    adjustment_type VARCHAR(30) NOT NULL,
    previous_state VARCHAR(2000) NOT NULL,
    new_state VARCHAR(2000) NOT NULL,
    applied_by BIGINT NOT NULL,
    applied_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_adjustment_request FOREIGN KEY (request_id) REFERENCES employee_requests(id),
    CONSTRAINT fk_adjustment_attendance FOREIGN KEY (attendance_day_id) REFERENCES attendance_days(id),
    CONSTRAINT fk_adjustment_user FOREIGN KEY (applied_by) REFERENCES users(id),
    CONSTRAINT uk_adjustment_request UNIQUE (request_id),
    INDEX ix_adjustment_attendance (attendance_day_id, applied_at)
);

CREATE UNIQUE INDEX uk_incident_attendance_type_source
    ON incidents (attendance_day_id, incident_type, source);
