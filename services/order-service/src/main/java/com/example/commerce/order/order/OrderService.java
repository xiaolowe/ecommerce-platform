package com.example.commerce.order.order;

import com.example.commerce.order.client.CatalogClient;
import com.example.commerce.order.client.InventoryClient;
import com.example.commerce.platform.common.error.BadRequestException;
import com.example.commerce.platform.common.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Transactional(readOnly = true)
    public Page<OrderResponse> listForAdmin(OrderStatus status, String keyword, Pageable pageable) {
        return orderRepository.searchForAdmin(status, normalizeKeyword(keyword), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> listForCustomer(String customerId, OrderStatus status, Pageable pageable) {
        return orderRepository.searchForCustomer(customerId, status, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public void markPaid(String orderId, OrderDtos.MarkOrderPaidRequest request) {
        if (request.amountCents() <= 0 || request.currency() == null
                || request.paymentNo() == null || request.paymentNo().isBlank()) {
            throw new BadRequestException("Invalid payment confirmation");
        }
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
        if (order.getTotalCents() != request.amountCents()
                || !order.getCurrency().equals(request.currency())) {
            throw new BadRequestException("Payment amount does not match the order");
        }
        try {
            order.markPaid(request.paymentNo(), Instant.now());
        } catch (IllegalStateException exception) {
            throw new BadRequestException(exception.getMessage());
        }
        orderRepository.saveAndFlush(order);
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
                order.getPaymentNo(),
                order.getPaidAt(),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }
}
