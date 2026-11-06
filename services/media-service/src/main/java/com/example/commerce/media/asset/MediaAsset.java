package com.example.commerce.media.asset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "media_assets")
public class MediaAsset {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "object_key", nullable = false, unique = true, length = 300)
    private String objectKey;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 64)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;

    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MediaAsset() {
    }

    public MediaAsset(String id, String objectKey, String originalName, String contentType,
                      long sizeBytes, int width, int height, String sha256, Instant createdAt) {
        this.id = id;
        this.objectKey = objectKey;
        this.originalName = originalName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.width = width;
        this.height = height;
        this.sha256 = sha256;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getSha256() {
        return sha256;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
