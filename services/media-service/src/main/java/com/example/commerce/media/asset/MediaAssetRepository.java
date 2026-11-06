package com.example.commerce.media.asset;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, String> {

    Page<MediaAsset> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<MediaAsset> findByObjectKey(String objectKey);
}
