package com.example.commerce.order.order;

import com.example.commerce.platform.common.error.UnauthorizedException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static com.example.commerce.order.order.OrderDtos.MarkOrderPaidRequest;

@RestController
@RequestMapping("/internal/orders")
public class InternalOrderPaymentController {

    private final OrderService orderService;
    private final byte[] internalApiKey;

    public InternalOrderPaymentController(OrderService orderService,
                                          @Value("${internal.api-key:local-internal-api-key-change-me}")
                                          String internalApiKey) {
        this.orderService = orderService;
        this.internalApiKey = internalApiKey.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping("/{id}/paid")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void markPaid(@PathVariable String id,
                  @RequestHeader(value = "X-Internal-Token", required = false) String token,
                  @Valid @RequestBody MarkOrderPaidRequest request) {
        if (token == null) {
            throw new UnauthorizedException("Invalid internal token");
        }
        byte[] supplied = token.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(internalApiKey, supplied)) {
            throw new UnauthorizedException("Invalid internal token");
        }
        orderService.markPaid(id, request);
    }
}
