package com.example.commerce.order.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, String> {

    Optional<CustomerOrder> findByIdAndCustomerId(String id, String customerId);

    List<CustomerOrder> findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
            OrderStatus status, Instant cutoff);
}
