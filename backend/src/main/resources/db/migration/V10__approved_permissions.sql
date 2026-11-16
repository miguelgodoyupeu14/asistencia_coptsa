CREATE TABLE approved_permissions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    affected_date DATE NOT NULL,
    requested_from TIMESTAMP(6) NOT NULL,
    requested_to TIMESTAMP(6) NOT NULL,
    approved_by BIGINT NOT NULL,
    approved_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_permission_request FOREIGN KEY (request_id) REFERENCES employee_requests(id),
    CONSTRAINT fk_permission_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_permission_approver FOREIGN KEY (approved_by) REFERENCES users(id),
    CONSTRAINT uk_permission_request UNIQUE (request_id),
    INDEX ix_permission_employee_date (employee_id, affected_date)
);
