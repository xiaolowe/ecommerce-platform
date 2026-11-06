CREATE TABLE media_assets (
    id VARCHAR(36) PRIMARY KEY,
    object_key VARCHAR(300) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(64) NOT NULL,
    size_bytes BIGINT NOT NULL,
    width INT NOT NULL,
    height INT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_media_assets_object_key UNIQUE (object_key)
);

CREATE INDEX idx_media_assets_created_at ON media_assets (created_at);
