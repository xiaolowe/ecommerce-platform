package com.example.commerce.order.order;

import com.example.commerce.order.client.CartClient;
import com.example.commerce.order.client.CatalogClient;
import com.example.commerce.order.client.InventoryClient;
import com.example.commerce.platform.common.error.BadRequestException;
import com.example.commerce.platform.common.error.ConflictException;
import com.example.commerce.platform.common.error.NotFoundException;
import com.example.commerce.platform.common.web.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.example.commerce.order.order.OrderDtos.CreateOrderRequest;
import static com.example.commerce.order.order.OrderDtos.OrderItemResponse;
import static com.example.commerce.order.order.OrderDtos.OrderResponse;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final CustomerOrderMapper orderMapper;
    private final CustomerAddressMapper addressMapper;
    private final CatalogClient catalogClient;
    private final InventoryClient inventoryClient;
    private final CartClient cartClient;
    private final TransactionTemplate transactionTemplate;
    private final long paymentTtlSeconds;

    public OrderService(CustomerOrderMapper orderMapper,
                        CustomerAddressMapper addressMapper,
                        CatalogClient catalogClient,
                        InventoryClient inventoryClient,
                        CartClient cartClient,
                        TransactionTemplate transactionTemplate,
                        @Value("${order.payment-ttl-seconds:900}") long paymentTtlSeconds) {
        this.orderMapper = orderMapper;
        this.addressMapper = addressMapper;
        this.catalogClient = catalogClient;
        this.inventoryClient = inventoryClient;
        this.cartClient = cartClient;
        this.transactionTemplate = transactionTemplate;
        this.paymentTtlSeconds = Math.max(1, paymentTtlSeconds);
    }

    public OrderResponse create(String customerId, String accessToken, CreateOrderRequest request) {
        CustomerAddress address = addressMapper.findByIdAndCustomerId(request.addressId(), customerId)
                .orElseThrow(() -> new NotFoundException("Address not found: " + request.addressId()));
        Map<String, Long> quantities = new LinkedHashMap<>();
        request.items().forEach(item -> quantities.merge(item.productId(), item.quantity(), Math::addExact));

        Map<String, CatalogClient.ProductSnapshot> products = new LinkedHashMap<>();
        long totalCents = 0;
        String currency = null;
        for (Map.Entry<String, Long> entry : quantities.entrySet()) {
            CatalogClient.ProductSnapshot product = catalogClient.getProduct(entry.getKey(), accessToken);
            if (!"ACTIVE".equals(product.status())) {
                throw new BadRequestException("Product is not available: " + entry.getKey());
            }
            if (currency != null && !currency.equals(product.currency())) {
                throw new BadRequestException("Products in one order must use the same currency");
            }
            currency = product.currency();
            totalCents = Math.addExact(
                    totalCents, Math.multiplyExact(product.priceCents(), entry.getValue()));
            products.put(entry.getKey(), product);
        }

        Instant now = Instant.now();
        CustomerOrder order = new CustomerOrder(
                UUID.randomUUID().toString(),
                customerId,
                totalCents,
                currency,
                now,
                now.plusSeconds(paymentTtlSeconds),
                address
        );
        products.forEach((productId, product) -> order.addItem(new OrderItem(
                productId, product.sku(), product.name(), product.priceCents(), quantities.get(productId))));
        orderMapper.insert(order);
        order.getItems().forEach(item -> orderMapper.insertItem(order.getId(), item));

        List<String> reservedProductIds = new java.util.ArrayList<>();
        try {
            for (OrderItem item : order.getItems()) {
                inventoryClient.reserve(order.getId(), item.getProductId(), item.getQuantity());
                reservedProductIds.add(item.getProductId());
            }
            order.confirmStock();
            orderMapper.update(order);
            completeCartCheckout(customerId, order.getItems());
            return toResponse(order);
        } catch (RuntimeException exception) {
            log.error("Order {} create flow failed", order.getId(), exception);
            compensate(order, reservedProductIds, exception);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse get(String customerId, String orderId) {
        return orderMapper.findByIdAndCustomerId(orderId, customerId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
    }

    @Transactional(readOnly = true)
    public PageResult<OrderResponse> listForAdmin(OrderStatus status, String keyword, int page, int size) {
        String normalized = normalizeKeyword(keyword);
        return PageResult.of(
                orderMapper.findAdminPage(status, normalized, page * size, size).stream()
                        .map(this::toResponse)
                        .toList(),
                page, size, orderMapper.countAdmin(status, normalized));
    }

    @Transactional(readOnly = true)
    public PageResult<OrderResponse> listForCustomer(String customerId, OrderStatus status, int page, int size) {
        return PageResult.of(
                orderMapper.findCustomerPage(customerId, status, page * size, size).stream()
                        .map(this::toResponse)
                        .toList(),
                page, size, orderMapper.countCustomer(customerId, status));
    }

    public OrderResponse cancel(String customerId, String orderId) {
        OrderCancellation cancellation = transactionTemplate.execute(status -> {
            CustomerOrder order = orderMapper.findByIdAndCustomerIdForUpdate(orderId, customerId)
                    .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
            return cancelLocked(order, "CUSTOMER_REQUEST");
        });
        if (cancellation == null) {
            throw new IllegalStateException("Order cancellation transaction did not return an order");
        }
        releaseStockIfNeeded(cancellation);
        return get(customerId, orderId);
    }

    public void cancelExpiredOrder(String orderId) {
        OrderCancellation cancellation = transactionTemplate.execute(status -> {
            CustomerOrder order = orderMapper.findByIdForUpdate(orderId)
                    .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
            if (order.getStatus() != OrderStatus.PENDING_PAYMENT
                    || order.getPaymentExpiresAt() == null
                    || order.getPaymentExpiresAt().isAfter(Instant.now())) {
                return null;
            }
            return cancelLocked(order, "PAYMENT_TIMEOUT");
        });
        if (cancellation != null) {
            log.info("Order {} cancelled after payment timeout", orderId);
            releaseStockIfNeeded(cancellation);
        }
    }

    public void retryPendingStockReleases() {
        orderMapper.findUnreleasedCancelled(100)
                .forEach(order -> releaseStockIfNeeded(new OrderCancellation(
                        order.getId(),
                        order.getItems().stream().map(OrderItem::getProductId).toList(),
                        order.isStockReleased())));
    }

    public List<CustomerOrder> findExpiredPaymentOrders() {
        return orderMapper.findExpiredPayments(Instant.now(), 100);
    }

    @Transactional
    public void markPaid(String orderId, OrderDtos.MarkOrderPaidRequest request) {
        if (request.amountCents() <= 0 || request.currency() == null
                || request.paymentNo() == null || request.paymentNo().isBlank()) {
            throw new BadRequestException("Invalid payment confirmation");
        }
        CustomerOrder order = orderMapper.findByIdForUpdate(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
        if (order.getTotalCents() != request.amountCents()
                || !order.getCurrency().equals(request.currency())) {
            throw new BadRequestException("Payment amount does not match the order");
        }
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT
                && order.getPaymentExpiresAt() != null
                && !order.getPaymentExpiresAt().isAfter(Instant.now())) {
            throw new ConflictException("Order payment window has expired");
        }
        try {
            order.markPaid(request.paymentNo(), Instant.now());
        } catch (IllegalStateException exception) {
            throw new ConflictException(exception.getMessage());
        }
        orderMapper.update(order);
    }

    private void compensate(CustomerOrder order, List<String> reservedProductIds, RuntimeException cause) {
        if (releaseReservations(order.getId(), reservedProductIds)) {
            order.markStockReleased(Instant.now());
        }
        order.cancel("CREATE_FAILED", Instant.now());
        orderMapper.update(order);
        log.warn("Order {} cancelled after create failure", order.getId(), cause);
    }

    private OrderCancellation cancelLocked(CustomerOrder order, String reason) {
        if (order.getStatus() == OrderStatus.PAID) {
            throw new ConflictException("A paid order cannot be cancelled");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return new OrderCancellation(
                    order.getId(),
                    order.getItems().stream().map(OrderItem::getProductId).toList(),
                    order.isStockReleased());
        }
        try {
            order.cancel(reason, Instant.now());
        } catch (IllegalStateException exception) {
            throw new ConflictException(exception.getMessage());
        }
        orderMapper.update(order);
        return new OrderCancellation(
                order.getId(),
                order.getItems().stream().map(OrderItem::getProductId).toList(),
                order.isStockReleased());
    }

    private void releaseStockIfNeeded(OrderCancellation cancellation) {
        if (cancellation.stockReleased()) {
            return;
        }
        if (releaseReservations(cancellation.orderId(), cancellation.productIds())) {
            transactionTemplate.executeWithoutResult(status -> {
                CustomerOrder order = orderMapper.findByIdForUpdate(cancellation.orderId())
                        .orElseThrow(() -> new NotFoundException("Order not found: " + cancellation.orderId()));
                if (!order.isStockReleased()) {
                    order.markStockReleased(Instant.now());
                    orderMapper.update(order);
                }
            });
        }
    }

    private boolean releaseReservations(String orderId, List<String> productIds) {
        boolean allReleased = true;
        for (String productId : productIds) {
            try {
                inventoryClient.release(orderId, productId);
            } catch (RuntimeException exception) {
                allReleased = false;
                log.warn("Inventory release pending for cancelled order {} product {}",
                        orderId, productId, exception);
            }
        }
        return allReleased;
    }

    private void completeCartCheckout(String customerId, List<OrderItem> items) {
        try {
            cartClient.completeCheckout(customerId, items.stream()
                    .map(item -> new CartClient.CheckedOutItem(item.getProductId(), item.getQuantity()))
                    .toList());
        } catch (RuntimeException exception) {
            log.warn("Order created but cart cleanup failed for customer {}", customerId, exception);
        }
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
                order.getPaymentExpiresAt(),
                order.getCancelledAt(),
                order.getCancellationReason(),
                order.isStockReleased(),
                new OrderDtos.ShippingAddressResponse(
                        order.getRecipientName(),
                        order.getRecipientPhone(),
                        order.getShippingRegion(),
                        order.getShippingDetail()),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    private record OrderCancellation(String orderId, List<String> productIds, boolean stockReleased) {
    }
}
