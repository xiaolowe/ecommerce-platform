package com.example.commerce.order.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class OrderReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(OrderReconciliationJob.class);

    private final CustomerOrderRepository orderRepository;

    public OrderReconciliationJob(CustomerOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Scheduled(fixedDelayString = "${jobs.order-reconciliation.delay-ms:120000}")
    public void cancelStaleStockPendingOrders() {
        Instant cutoff = Instant.now().minus(2, ChronoUnit.MINUTES);
        orderRepository.findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(OrderStatus.PENDING_STOCK, cutoff)
                .forEach(order -> {
                    try {
                        // A system-to-system token is required for production recovery.
                        log.warn("Order {} is still pending stock; manual or service-token recovery required",
                                order.getId());
                    } catch (RuntimeException exception) {
                        log.error("Failed to recover order {}", order.getId(), exception);
                    }
                });
    }
}
