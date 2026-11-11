package com.example.commerce.media.asset;

import com.example.commerce.media.storage.LocalMediaStorage;
import com.example.commerce.platform.common.error.NotFoundException;
import com.example.commerce.platform.common.web.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

@Service
public class MediaAssetService {

    private final MediaAssetMapper mapper;
    private final LocalMediaStorage storage;

    public MediaAssetService(MediaAssetMapper mapper, LocalMediaStorage storage) {
        this.mapper = mapper;
        this.storage = storage;
    }

    @Transactional
    public MediaAsset upload(MultipartFile file) {
        LocalMediaStorage.StoredImage stored = storage.store(file);
        try {
            MediaAsset asset = new MediaAsset(
                    UUID.randomUUID().toString(),
                    stored.objectKey(),
                    stored.originalName(),
                    stored.contentType(),
                    stored.sizeBytes(),
                    stored.width(),
                    stored.height(),
                    stored.sha256(),
                    Instant.now()
            );
            mapper.insert(asset);
            return asset;
        } catch (RuntimeException exception) {
            storage.delete(stored.objectKey());
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public PageResult<MediaAsset> list(int page, int size) {
        return PageResult.of(mapper.findPage(page * size, size), page, size, mapper.count());
    }

    @Transactional(readOnly = true)
    public MediaAsset getByObjectKey(String objectKey) {
        return mapper.findByObjectKey(objectKey)
                .orElseThrow(() -> new NotFoundException("图片不存在"));
    }

    @Transactional
    public void delete(String id) {
        MediaAsset asset = mapper.findById(id)
                .orElseThrow(() -> new NotFoundException("图片不存在"));
        storage.delete(asset.getObjectKey());
        mapper.deleteById(id);
    }
}
