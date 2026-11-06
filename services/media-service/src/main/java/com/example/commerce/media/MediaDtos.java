package com.example.commerce.media;

import com.example.commerce.media.storage.LocalMediaStorage;

import java.time.Instant;

public final class MediaDtos {

    private MediaDtos() {
    }

    public record MediaAssetResponse(
            String id,
            String objectKey,
            String url,
            String originalName,
            String contentType,
            long sizeBytes,
            int width,
            int height,
            Instant createdAt
    ) {
        public static MediaAssetResponse from(com.example.commerce.media.asset.MediaAsset asset,
                                              LocalMediaStorage storage) {
            return new MediaAssetResponse(
                    asset.getId(),
                    asset.getObjectKey(),
                    storage.publicUrl(asset.getObjectKey()),
                    asset.getOriginalName(),
                    asset.getContentType(),
                    asset.getSizeBytes(),
                    asset.getWidth(),
                    asset.getHeight(),
                    asset.getCreatedAt()
            );
        }
    }
}
