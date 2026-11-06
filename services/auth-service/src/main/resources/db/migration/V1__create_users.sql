CREATE TABLE app_users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    role VARCHAR(24) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_app_users_email UNIQUE (email)
);
