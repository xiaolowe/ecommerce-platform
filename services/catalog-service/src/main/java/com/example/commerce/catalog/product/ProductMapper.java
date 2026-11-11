package com.example.commerce.catalog.product;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Mapper
public interface ProductMapper {

    List<Product> findPage(@Param("offset") int offset, @Param("limit") int limit);

    long count();

    Optional<Product> findById(String id);

    Optional<Product> findActiveById(String id);

    int countBySku(String sku);

    int insert(Product product);

    int updateImage(@Param("id") String id,
                    @Param("imageUrl") String imageUrl,
                    @Param("updatedAt") Instant updatedAt);
}
