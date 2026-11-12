package com.example.commerce.cart.cart;

import com.example.commerce.platform.common.error.UnauthorizedException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static com.example.commerce.cart.cart.CartDtos.CheckoutCartRequest;

@RestController
@RequestMapping("/internal/cart")
public class InternalCartController {

    private final CartService cartService;
    private final byte[] internalApiKey;

    public InternalCartController(CartService cartService,
                                  @Value("${internal.api-key:local-internal-api-key-change-me}") String internalApiKey) {
        this.cartService = cartService;
        this.internalApiKey = internalApiKey.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping("/checkout-complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void completeCheckout(@RequestHeader(value = "X-Internal-Token", required = false) String token,
                          @Valid @RequestBody CheckoutCartRequest request) {
        if (token == null
                || !MessageDigest.isEqual(internalApiKey, token.getBytes(StandardCharsets.UTF_8))) {
            throw new UnauthorizedException("Invalid internal token");
        }
        cartService.completeCheckout(request.customerId(), request.items());
    }
}
