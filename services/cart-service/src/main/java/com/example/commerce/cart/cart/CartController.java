package com.example.commerce.cart.cart;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.example.commerce.cart.cart.CartDtos.AddCartItemRequest;
import static com.example.commerce.cart.cart.CartDtos.CartResponse;
import static com.example.commerce.cart.cart.CartDtos.UpdateCartItemRequest;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    CartResponse get(@AuthenticationPrincipal Jwt jwt) {
        return cartService.get(jwt.getSubject());
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    CartResponse add(@AuthenticationPrincipal Jwt jwt,
                     @Valid @RequestBody AddCartItemRequest request) {
        return cartService.add(jwt.getSubject(), jwt.getTokenValue(), request);
    }

    @PutMapping("/items/{productId}")
    CartResponse updateQuantity(@AuthenticationPrincipal Jwt jwt,
                                @PathVariable String productId,
                                @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateQuantity(jwt.getSubject(), productId, request.quantity());
    }

    @DeleteMapping("/items/{productId}")
    CartResponse remove(@AuthenticationPrincipal Jwt jwt, @PathVariable String productId) {
        return cartService.remove(jwt.getSubject(), productId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void clear(@AuthenticationPrincipal Jwt jwt) {
        cartService.clear(jwt.getSubject());
    }
}
