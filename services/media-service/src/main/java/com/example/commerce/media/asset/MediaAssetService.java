package com.example.commerce.media.asset;

import com.example.commerce.media.storage.LocalMediaStorage;
import com.example.commerce.platform.common.error.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

@Service
public class MediaAssetService {

    private final MediaAssetRepository repository;
    private final LocalMediaStorage storage;

    public MediaAssetService(MediaAssetRepository repository, LocalMediaStorage storage) {
        this.repository = repository;
        this.storage = storage;
    }

    @Transactional
    public MediaAsset upload(MultipartFile file) {
        LocalMediaStorage.StoredImage stored = storage.store(file);
        try {
            return repository.save(new MediaAsset(
                    UUID.randomUUID().toString(),
                    stored.objectKey(),
                    stored.originalName(),
                    stored.contentType(),
                    stored.sizeBytes(),
                    stored.width(),
                    stored.height(),
                    stored.sha256(),
                    Instant.now()
            ));
        } catch (RuntimeException exception) {
            storage.delete(stored.objectKey());
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public Page<MediaAsset> list(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public MediaAsset getByObjectKey(String objectKey) {
        return repository.findByObjectKey(objectKey)
                .orElseThrow(() -> new NotFoundException("图片不存在"));
    }

    @Transactional
    public void delete(String id) {
        MediaAsset asset = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("图片不存在"));
        storage.delete(asset.getObjectKey());
        repository.delete(asset);
    }
}
