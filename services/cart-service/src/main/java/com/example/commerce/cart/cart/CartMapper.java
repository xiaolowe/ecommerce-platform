package com.example.commerce.cart.cart;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CartMapper {

    List<CartItem> findByCustomerId(String customerId);

    Optional<CartItem> findByCustomerIdAndProductId(
            @Param("customerId") String customerId,
            @Param("productId") String productId);

    Optional<CartItem> findByCustomerIdAndProductIdForUpdate(
            @Param("customerId") String customerId,
            @Param("productId") String productId);

    int insert(CartItem item);

    int updateItem(CartItem item);

    int updateQuantity(@Param("customerId") String customerId,
                       @Param("productId") String productId,
                       @Param("quantity") long quantity,
                       @Param("updatedAt") java.time.Instant updatedAt);

    int decrementQuantity(@Param("customerId") String customerId,
                          @Param("productId") String productId,
                          @Param("quantity") long quantity,
                          @Param("updatedAt") java.time.Instant updatedAt);

    int deleteExactQuantity(@Param("customerId") String customerId,
                            @Param("productId") String productId,
                            @Param("quantity") long quantity);

    int deleteItem(@Param("customerId") String customerId,
                   @Param("productId") String productId);

    int deleteByCustomerId(String customerId);
}
