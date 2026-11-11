package com.example.commerce.order.order;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Mapper
public interface CustomerOrderMapper {

    int insert(CustomerOrder order);

    int insertItem(@Param("orderId") String orderId, @Param("item") OrderItem item);

    int update(CustomerOrder order);

    List<OrderItem> findItemsByOrderId(@Param("orderId") String orderId);

    Optional<CustomerOrder> findByIdAndCustomerId(
            @Param("id") String id, @Param("customerId") String customerId);

    Optional<CustomerOrder> findByIdForUpdate(String id);

    Optional<CustomerOrder> findByIdAndCustomerIdForUpdate(
            @Param("id") String id, @Param("customerId") String customerId);

    List<CustomerOrder> findAdminPage(
            @Param("status") OrderStatus status,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("limit") int limit);

    long countAdmin(@Param("status") OrderStatus status, @Param("keyword") String keyword);

    List<CustomerOrder> findCustomerPage(
            @Param("customerId") String customerId,
            @Param("status") OrderStatus status,
            @Param("offset") int offset,
            @Param("limit") int limit);

    long countCustomer(@Param("customerId") String customerId,
                       @Param("status") OrderStatus status);

    List<CustomerOrder> findPendingStockBefore(@Param("cutoff") Instant cutoff,
                                                @Param("limit") int limit);

    List<CustomerOrder> findExpiredPayments(@Param("now") Instant now, @Param("limit") int limit);

    List<CustomerOrder> findUnreleasedCancelled(@Param("limit") int limit);
}
