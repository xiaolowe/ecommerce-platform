package com.example.commerce.media.asset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MediaAssetMapperTest {

    @Autowired
    private MediaAssetMapper assetMapper;

    @Test
    void insertsAndQueriesMediaAsset() {
        MediaAsset asset = new MediaAsset(
                "asset-id",
                "images/asset.png",
                "asset.png",
                "image/png",
                128,
                16,
                12,
                "checksum",
                Instant.now()
        );

        assetMapper.insert(asset);

        assertThat(assetMapper.findByObjectKey("images/asset.png"))
                .get()
                .extracting(MediaAsset::getOriginalName)
                .isEqualTo("asset.png");
        assertThat(assetMapper.findPage(0, 10)).hasSize(1);
        assertThat(assetMapper.count()).isEqualTo(1);
    }
}
