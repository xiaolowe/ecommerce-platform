package com.example.commerce.inventory.reservation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockReservationRepository extends JpaRepository<StockReservation, String> {

    Optional<StockReservation> findByOrderIdAndProductId(String orderId, String productId);
}
