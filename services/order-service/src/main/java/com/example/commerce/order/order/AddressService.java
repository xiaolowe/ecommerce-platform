package com.example.commerce.order.order;

import com.example.commerce.platform.common.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.example.commerce.order.order.AddressDtos.AddressResponse;
import static com.example.commerce.order.order.AddressDtos.SaveAddressRequest;

@Service
public class AddressService {

    private final CustomerAddressMapper addressMapper;

    public AddressService(CustomerAddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(String customerId) {
        return addressMapper.findByCustomerId(customerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AddressResponse create(String customerId, SaveAddressRequest request) {
        boolean firstAddress = addressMapper.findByCustomerId(customerId).isEmpty();
        boolean makeDefault = request.defaultAddress() || firstAddress;
        if (makeDefault) {
            addressMapper.clearDefault(customerId);
        }
        Instant now = Instant.now();
        CustomerAddress address = new CustomerAddress(
                UUID.randomUUID().toString(),
                customerId,
                request.recipientName().trim(),
                request.recipientPhone().trim(),
                request.region().trim(),
                request.detail().trim(),
                makeDefault,
                now,
                now
        );
        addressMapper.insert(address);
        return toResponse(address);
    }

    @Transactional
    public AddressResponse update(String customerId, String id, SaveAddressRequest request) {
        CustomerAddress address = addressMapper.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new NotFoundException("Address not found: " + id));
        if (request.defaultAddress()) {
            addressMapper.clearDefault(customerId);
        }
        address.setRecipientName(request.recipientName().trim());
        address.setRecipientPhone(request.recipientPhone().trim());
        address.setRegion(request.region().trim());
        address.setDetail(request.detail().trim());
        address.setDefaultAddress(request.defaultAddress());
        address.setUpdatedAt(Instant.now());
        addressMapper.update(address);
        return toResponse(address);
    }

    @Transactional
    public AddressResponse setDefault(String customerId, String id) {
        CustomerAddress address = addressMapper.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new NotFoundException("Address not found: " + id));
        addressMapper.clearDefault(customerId);
        address.setDefaultAddress(true);
        address.setUpdatedAt(Instant.now());
        addressMapper.update(address);
        return toResponse(address);
    }

    @Transactional
    public void delete(String customerId, String id) {
        CustomerAddress address = addressMapper.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new NotFoundException("Address not found: " + id));
        addressMapper.deleteByIdAndCustomerId(id, customerId);
        if (address.isDefaultAddress()) {
            addressMapper.findByCustomerId(customerId).stream().findFirst().ifPresent(next -> {
                next.setDefaultAddress(true);
                next.setUpdatedAt(Instant.now());
                addressMapper.update(next);
            });
        }
    }

    AddressResponse toResponse(CustomerAddress address) {
        return new AddressResponse(
                address.getId(),
                address.getRecipientName(),
                address.getRecipientPhone(),
                address.getRegion(),
                address.getDetail(),
                address.isDefaultAddress(),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }
}
