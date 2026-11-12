package com.example.commerce.order.order;

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

import java.util.List;

import static com.example.commerce.order.order.AddressDtos.AddressResponse;
import static com.example.commerce.order.order.AddressDtos.SaveAddressRequest;

@RestController
@RequestMapping("/api/v1/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    List<AddressResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return addressService.list(jwt.getSubject());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AddressResponse create(@AuthenticationPrincipal Jwt jwt,
                           @Valid @RequestBody SaveAddressRequest request) {
        return addressService.create(jwt.getSubject(), request);
    }

    @PutMapping("/{id}")
    AddressResponse update(@AuthenticationPrincipal Jwt jwt,
                           @PathVariable String id,
                           @Valid @RequestBody SaveAddressRequest request) {
        return addressService.update(jwt.getSubject(), id, request);
    }

    @PostMapping("/{id}/default")
    AddressResponse setDefault(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return addressService.setDefault(jwt.getSubject(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        addressService.delete(jwt.getSubject(), id);
    }
}
