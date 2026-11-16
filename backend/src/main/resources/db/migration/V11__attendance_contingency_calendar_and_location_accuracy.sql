ALTER TABLE punches ADD COLUMN site_id BIGINT NULL;
ALTER TABLE punches ADD COLUMN created_by BIGINT NULL;
ALTER TABLE punches ADD COLUMN reason_code VARCHAR(40) NULL;
ALTER TABLE punches ADD COLUMN request_id BIGINT NULL;
ALTER TABLE punches ADD COLUMN accuracy_meters DECIMAL(8,2) NULL;
ALTER TABLE punches ADD CONSTRAINT fk_punch_site FOREIGN KEY (site_id) REFERENCES sites(id);
ALTER TABLE punches ADD CONSTRAINT fk_punch_created_by FOREIGN KEY (created_by) REFERENCES users(id);
ALTER TABLE punches ADD CONSTRAINT fk_punch_request FOREIGN KEY (request_id) REFERENCES employee_requests(id);

ALTER TABLE punch_attempts ADD COLUMN accuracy_meters DECIMAL(8,2) NULL;

ALTER TABLE registered_devices ADD COLUMN revoked_by BIGINT NULL;
ALTER TABLE registered_devices ADD COLUMN revoked_at TIMESTAMP(6) NULL;
ALTER TABLE registered_devices ADD COLUMN revocation_reason VARCHAR(255) NULL;
ALTER TABLE registered_devices ADD CONSTRAINT fk_device_revoker FOREIGN KEY (revoked_by) REFERENCES users(id);

ALTER TABLE employee_assignments ADD COLUMN assignment_type VARCHAR(30) NOT NULL DEFAULT 'PERMANENT';
ALTER TABLE employee_assignments ADD COLUMN assignment_reason VARCHAR(500) NULL;

CREATE TABLE attendance_calendar (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    site_id BIGINT NOT NULL,
    calendar_date DATE NOT NULL,
    day_type VARCHAR(30) NOT NULL,
    start_time TIME NULL,
    end_time TIME NULL,
    description VARCHAR(255) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_calendar_site FOREIGN KEY (site_id) REFERENCES sites(id),
    CONSTRAINT fk_calendar_creator FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT uk_calendar_site_date UNIQUE (site_id, calendar_date),
    CONSTRAINT ck_calendar_type CHECK (day_type IN ('HOLIDAY','NON_WORKING_DAY','SPECIAL_WORKING_DAY','SPECIAL_SCHEDULE')),
    INDEX ix_calendar_date (calendar_date, active)
);

CREATE TABLE attendance_business_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    attendance_day_id BIGINT NULL,
    punch_id BIGINT NULL,
    employee_id BIGINT NOT NULL,
    actor_user_id BIGINT NULL,
    event_type VARCHAR(40) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    result_status VARCHAR(20) NOT NULL,
    reason_code VARCHAR(40) NULL,
    detail VARCHAR(1000) NULL,
    effective_at TIMESTAMP(6) NULL,
    recorded_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_business_event_day FOREIGN KEY (attendance_day_id) REFERENCES attendance_days(id),
    CONSTRAINT fk_business_event_punch FOREIGN KEY (punch_id) REFERENCES punches(id),
    CONSTRAINT fk_business_event_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_business_event_actor FOREIGN KEY (actor_user_id) REFERENCES users(id),
    INDEX ix_business_event_employee_time (employee_id, recorded_at),
    INDEX ix_business_event_type_time (event_type, recorded_at)
);
