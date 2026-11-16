CREATE TABLE sites (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    address VARCHAR(255),
    time_zone VARCHAR(60) NOT NULL DEFAULT 'America/Lima',
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE areas (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE positions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE employees (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL UNIQUE,
    employee_code VARCHAR(40) NOT NULL UNIQUE,
    document_type VARCHAR(20) NOT NULL,
    document_number VARCHAR(40) NOT NULL,
    first_names VARCHAR(150) NOT NULL,
    last_names VARCHAR(150) NOT NULL,
    email VARCHAR(150),
    phone VARCHAR(40),
    hire_date DATE NOT NULL,
    termination_date DATE NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_employee_document UNIQUE (document_type, document_number),
    CONSTRAINT fk_employee_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE employee_assignments (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    site_id BIGINT NOT NULL,
    area_id BIGINT NOT NULL,
    position_id BIGINT NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_assignment_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT fk_assignment_site FOREIGN KEY (site_id) REFERENCES sites (id),
    CONSTRAINT fk_assignment_area FOREIGN KEY (area_id) REFERENCES areas (id),
    CONSTRAINT fk_assignment_position FOREIGN KEY (position_id) REFERENCES positions (id),
    INDEX ix_assignment_employee_dates (employee_id, valid_from, valid_to)
);
