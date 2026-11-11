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

    private final CustomerOrderMapper orderMapper;
    private final OrderService orderService;

    public OrderReconciliationJob(CustomerOrderMapper orderMapper,
                                  OrderService orderService) {
        this.orderMapper = orderMapper;
        this.orderService = orderService;
    }

    @Scheduled(fixedDelayString = "${jobs.order-expiration.delay-ms:30000}")
    public void reconcileOrders() {
        orderService.findExpiredPaymentOrders().forEach(order -> {
            try {
                orderService.cancelExpiredOrder(order.getId());
            } catch (RuntimeException exception) {
                log.error("Failed to cancel expired order {}", order.getId(), exception);
            }
        });
        try {
            orderService.retryPendingStockReleases();
        } catch (RuntimeException exception) {
            log.error("Failed to retry cancelled order inventory releases", exception);
        }

        Instant cutoff = Instant.now().minus(2, ChronoUnit.MINUTES);
        orderMapper.findPendingStockBefore(cutoff, 100)
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
