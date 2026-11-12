package com.example.commerce.cart.cart;

import com.example.commerce.cart.client.CatalogClient;
import com.example.commerce.platform.common.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.example.commerce.cart.cart.CartDtos.AddCartItemRequest;
import static com.example.commerce.cart.cart.CartDtos.CartItemResponse;
import static com.example.commerce.cart.cart.CartDtos.CartResponse;

@Service
public class CartService {

    private final CartMapper cartMapper;
    private final CatalogClient catalogClient;
    private final TransactionTemplate transactionTemplate;

    public CartService(CartMapper cartMapper,
                       CatalogClient catalogClient,
                       TransactionTemplate transactionTemplate) {
        this.cartMapper = cartMapper;
        this.catalogClient = catalogClient;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional(readOnly = true)
    public CartResponse get(String customerId) {
        return toResponse(cartMapper.findByCustomerId(customerId));
    }

    public CartResponse add(String customerId, String accessToken, AddCartItemRequest request) {
        CatalogClient.ProductSnapshot product = catalogClient.getProduct(request.productId(), accessToken);
        transactionTemplate.executeWithoutResult(status -> {
            Instant now = Instant.now();
            CartItem existing = cartMapper
                    .findByCustomerIdAndProductIdForUpdate(customerId, request.productId())
                    .orElse(null);
            if (existing == null) {
                cartMapper.insert(new CartItem(
                        UUID.randomUUID().toString(),
                        customerId,
                        product.id(),
                        product.sku(),
                        product.name(),
                        product.priceCents(),
                        product.currency(),
                        request.quantity(),
                        now,
                        now
                ));
                return;
            }
            existing.setSku(product.sku());
            existing.setProductName(product.name());
            existing.setUnitPriceCents(product.priceCents());
            existing.setCurrency(product.currency());
            existing.setQuantity(Math.addExact(existing.getQuantity(), request.quantity()));
            existing.setUpdatedAt(now);
            cartMapper.updateItem(existing);
        });
        return get(customerId);
    }

    public CartResponse updateQuantity(String customerId, String productId, long quantity) {
        transactionTemplate.executeWithoutResult(status -> {
            cartMapper.findByCustomerIdAndProductIdForUpdate(customerId, productId)
                    .orElseThrow(() -> new NotFoundException("Cart item not found: " + productId));
            cartMapper.updateQuantity(customerId, productId, quantity, Instant.now());
        });
        return get(customerId);
    }

    public CartResponse remove(String customerId, String productId) {
        cartMapper.deleteItem(customerId, productId);
        return get(customerId);
    }

    @Transactional
    public void clear(String customerId) {
        cartMapper.deleteByCustomerId(customerId);
    }

    @Transactional
    public void completeCheckout(String customerId, List<CartDtos.CheckedOutItem> items) {
        Instant now = Instant.now();
        for (CartDtos.CheckedOutItem item : items) {
            int decremented = cartMapper.decrementQuantity(
                    customerId, item.productId(), item.quantity(), now);
            if (decremented == 0) {
                cartMapper.deleteExactQuantity(customerId, item.productId(), item.quantity());
            }
        }
    }

    private CartResponse toResponse(List<CartItem> items) {
        List<CartItemResponse> responses = items.stream()
                .map(item -> new CartItemResponse(
                        item.getId(),
                        item.getProductId(),
                        item.getSku(),
                        item.getProductName(),
                        item.getUnitPriceCents(),
                        item.getCurrency(),
                        item.getQuantity(),
                        Math.multiplyExact(item.getUnitPriceCents(), item.getQuantity()),
                        item.getUpdatedAt()
                ))
                .toList();
        long totalQuantity = items.stream().mapToLong(CartItem::getQuantity).sum();
        long subtotal = responses.stream().mapToLong(CartItemResponse::subtotalCents).sum();
        Instant updatedAt = items.stream()
                .map(CartItem::getUpdatedAt)
                .max(Instant::compareTo)
                .orElse(null);
        return new CartResponse(responses, totalQuantity, subtotal, updatedAt);
    }
}
