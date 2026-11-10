package com.example.commerce.order.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, String> {

    Optional<CustomerOrder> findByIdAndCustomerId(String id, String customerId);

    List<CustomerOrder> findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
            OrderStatus status, Instant cutoff);

    @Query("""
            select orders from CustomerOrder orders
            where (:status is null or orders.status = :status)
              and (:keyword is null
                   or lower(orders.id) like lower(concat('%', :keyword, '%'))
                   or lower(orders.customerId) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(orders.paymentNo, '')) like lower(concat('%', :keyword, '%')))
            """)
    Page<CustomerOrder> searchForAdmin(@Param("status") OrderStatus status,
                                       @Param("keyword") String keyword,
                                       Pageable pageable);

    @Query("""
            select orders from CustomerOrder orders
            where orders.customerId = :customerId
              and (:status is null or orders.status = :status)
            """)
    Page<CustomerOrder> searchForCustomer(@Param("customerId") String customerId,
                                          @Param("status") OrderStatus status,
                                          Pageable pageable);
}
