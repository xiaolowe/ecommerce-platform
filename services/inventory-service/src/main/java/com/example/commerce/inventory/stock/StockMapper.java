package com.example.commerce.inventory.stock;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.Optional;

@Mapper
public interface StockMapper {

    int countById(String productId);

    Optional<Stock> findById(String productId);

    int insert(Stock stock);

    int deleteAll();

    int reserve(@Param("productId") String productId,
                @Param("quantity") long quantity,
                @Param("updatedAt") Instant updatedAt);

    int release(@Param("productId") String productId,
                @Param("quantity") long quantity,
                @Param("updatedAt") Instant updatedAt);
}
