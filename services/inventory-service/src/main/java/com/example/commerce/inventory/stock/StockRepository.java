package com.example.commerce.inventory.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface StockRepository extends JpaRepository<Stock, String> {

    @Modifying(clearAutomatically = true)
    @Query("""
            update Stock s
               set s.availableStock = s.availableStock - :quantity,
                   s.reservedStock = s.reservedStock + :quantity,
                   s.updatedAt = :updatedAt,
                   s.version = s.version + 1
             where s.productId = :productId
               and s.availableStock >= :quantity
            """)
    int reserve(@Param("productId") String productId,
                @Param("quantity") long quantity,
                @Param("updatedAt") Instant updatedAt);

    @Modifying(clearAutomatically = true)
    @Query("""
            update Stock s
               set s.availableStock = s.availableStock + :quantity,
                   s.reservedStock = s.reservedStock - :quantity,
                   s.updatedAt = :updatedAt,
                   s.version = s.version + 1
             where s.productId = :productId
               and s.reservedStock >= :quantity
            """)
    int release(@Param("productId") String productId,
                @Param("quantity") long quantity,
                @Param("updatedAt") Instant updatedAt);
}
