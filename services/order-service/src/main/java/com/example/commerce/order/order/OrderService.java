package com.example.commerce.order.order;

import com.example.commerce.order.client.CatalogClient;
import com.example.commerce.order.client.InventoryClient;
import com.example.commerce.platform.common.error.BadRequestException;
import com.example.commerce.platform.common.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.example.commerce.order.order.OrderDtos.CreateOrderRequest;
import static com.example.commerce.order.order.OrderDtos.OrderItemResponse;
import static com.example.commerce.order.order.OrderDtos.OrderResponse;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final CustomerOrderRepository orderRepository;
    private final CatalogClient catalogClient;
    private final InventoryClient inventoryClient;

    public OrderService(CustomerOrderRepository orderRepository,
                        CatalogClient catalogClient,
                        InventoryClient inventoryClient) {
        this.orderRepository = orderRepository;
        this.catalogClient = catalogClient;
        this.inventoryClient = inventoryClient;
    }

    public OrderResponse create(String customerId, String accessToken, CreateOrderRequest request) {
        CatalogClient.ProductSnapshot product = catalogClient.getProduct(request.productId(), accessToken);
        if (!"ACTIVE".equals(product.status())) {
            throw new BadRequestException("Product is not available");
        }

        long totalCents = Math.multiplyExact(product.priceCents(), request.quantity());
        CustomerOrder order = new CustomerOrder(
                UUID.randomUUID().toString(),
                customerId,
                totalCents,
                product.currency(),
                Instant.now()
        );
        order.addItem(new OrderItem(
                product.id(), product.sku(), product.name(), product.priceCents(), request.quantity()));
        CustomerOrder savedOrder = orderRepository.saveAndFlush(order);

        try {
            inventoryClient.reserve(savedOrder.getId(), request.productId(), request.quantity(), accessToken);
            savedOrder.confirmStock();
            return toResponse(orderRepository.saveAndFlush(savedOrder));
        } catch (RuntimeException exception) {
            log.error("Order {} create flow failed", savedOrder.getId(), exception);
            compensate(savedOrder, accessToken, exception);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse get(String customerId, String orderId) {
        return orderRepository.findByIdAndCustomerId(orderId, customerId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
    }

    private void compensate(CustomerOrder order, String accessToken, RuntimeException cause) {
        try {
            inventoryClient.release(order.getId(), order.getItems().getFirst().getProductId(), accessToken);
        } catch (RuntimeException releaseFailure) {
            log.error("Failed to release inventory for order {}", order.getId(), releaseFailure);
        }
        order.cancel();
        orderRepository.saveAndFlush(order);
        log.warn("Order {} cancelled after create failure", order.getId(), cause);
    }

    private OrderResponse toResponse(CustomerOrder order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProductId(),
                        item.getSku(),
                        item.getProductName(),
                        item.getUnitPriceCents(),
                        item.getQuantity()))
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus().name(),
                order.getTotalCents(),
                order.getCurrency(),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
