package com.example.commerce.cart.cart;

import com.example.commerce.cart.client.CatalogClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.example.commerce.cart.cart.CartDtos.AddCartItemRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
class CartServiceTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartMapper cartMapper;

    @MockBean
    private CatalogClient catalogClient;

    @BeforeEach
    void clearCart() {
        cartMapper.deleteByCustomerId("customer-1");
        cartMapper.deleteByCustomerId("customer-2");
    }

    @Test
    void addsUpdatesAndIsolatesCartByCustomer() {
        when(catalogClient.getProduct("product-1", "token"))
                .thenReturn(new CatalogClient.ProductSnapshot(
                        "product-1", "SKU-1", "Test Product", 1999, "CNY", "ACTIVE"));

        cartService.add("customer-1", "token", new AddCartItemRequest("product-1", 2));
        cartService.add("customer-1", "token", new AddCartItemRequest("product-1", 1));

        var cart = cartService.get("customer-1");
        assertThat(cart.totalQuantity()).isEqualTo(3);
        assertThat(cart.subtotalCents()).isEqualTo(5997);
        assertThat(cartService.get("customer-2").items()).isEmpty();

        var updated = cartService.updateQuantity("customer-1", "product-1", 5);
        assertThat(updated.totalQuantity()).isEqualTo(5);

        var removed = cartService.remove("customer-1", "product-1");
        assertThat(removed.items()).isEmpty();
    }
}
