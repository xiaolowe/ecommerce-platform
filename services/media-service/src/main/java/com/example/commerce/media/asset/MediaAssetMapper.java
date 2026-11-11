package com.example.commerce.media.asset;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface MediaAssetMapper {

    int insert(MediaAsset asset);

    List<MediaAsset> findPage(@Param("offset") int offset, @Param("limit") int limit);

    long count();

    Optional<MediaAsset> findById(String id);

    Optional<MediaAsset> findByObjectKey(String objectKey);

    int deleteById(String id);
}
