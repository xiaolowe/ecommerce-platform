package com.example.commerce.inventory.reservation;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.Optional;

@Mapper
public interface StockReservationMapper {

    Optional<StockReservation> findByOrderIdAndProductId(
            @Param("orderId") String orderId, @Param("productId") String productId);

    Optional<StockReservation> findByOrderIdAndProductIdForUpdate(
            @Param("orderId") String orderId, @Param("productId") String productId);

    int insert(StockReservation reservation);

    int updateRelease(@Param("id") String id, @Param("releasedAt") Instant releasedAt);

    int deleteAll();
}
