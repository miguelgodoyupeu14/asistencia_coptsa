CREATE TABLE schedules (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE shifts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    schedule_id BIGINT NOT NULL,
    day_of_week SMALLINT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    crosses_midnight BOOLEAN NOT NULL DEFAULT FALSE,
    break_minutes INT NOT NULL DEFAULT 0,
    CONSTRAINT ck_shift_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_shift_break CHECK (break_minutes >= 0),
    CONSTRAINT fk_shift_schedule FOREIGN KEY (schedule_id) REFERENCES schedules (id),
    CONSTRAINT uk_shift_schedule_day UNIQUE (schedule_id, day_of_week)
);

CREATE TABLE schedule_assignments (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    schedule_id BIGINT NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_schedule_assignment_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT fk_schedule_assignment_schedule FOREIGN KEY (schedule_id) REFERENCES schedules (id),
    INDEX ix_schedule_assignment_dates (employee_id, valid_from, valid_to)
);

CREATE TABLE attendance_days (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    work_date DATE NOT NULL,
    shift_id BIGINT NULL,
    first_entry_at TIMESTAMP(6) NULL,
    last_exit_at TIMESTAMP(6) NULL,
    worked_minutes INT NOT NULL DEFAULT 0,
    late_minutes INT NOT NULL DEFAULT 0,
    early_exit_minutes INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL,
    calculation_version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_attendance_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT fk_attendance_shift FOREIGN KEY (shift_id) REFERENCES shifts (id),
    CONSTRAINT uk_attendance_employee_date UNIQUE (employee_id, work_date)
);

CREATE TABLE punches (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    attendance_day_id BIGINT NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    punch_type VARCHAR(20) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    ip_address VARCHAR(64),
    device_identifier VARCHAR(150),
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    qr_reference VARCHAR(150),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_punch_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT fk_punch_attendance FOREIGN KEY (attendance_day_id) REFERENCES attendance_days (id),
    INDEX ix_punch_employee_time (employee_id, occurred_at)
);
