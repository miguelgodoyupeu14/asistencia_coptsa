CREATE TABLE supervisor_scopes (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    site_id BIGINT NOT NULL,
    area_id BIGINT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_supervisor_scope_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_supervisor_scope_site FOREIGN KEY (site_id) REFERENCES sites(id),
    CONSTRAINT fk_supervisor_scope_area FOREIGN KEY (area_id) REFERENCES areas(id),
    CONSTRAINT uk_supervisor_scope UNIQUE (user_id, site_id, area_id),
    INDEX ix_supervisor_scope_user (user_id, active)
);
