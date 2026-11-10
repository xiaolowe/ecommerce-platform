package com.example.commerce.payment.provider;

import com.example.commerce.platform.common.error.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentProviderRegistry {

    private final Map<String, PaymentProvider> providers;

    public PaymentProviderRegistry(java.util.List<PaymentProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        provider -> normalize(provider.channel()),
                        Function.identity()));
    }

    public PaymentProvider require(String channel) {
        String normalized = normalize(channel);
        PaymentProvider provider = providers.get(normalized);
        if (provider == null) {
            throw new BadRequestException("Unsupported payment channel: " + normalized);
        }
        return provider;
    }

    private static String normalize(String channel) {
        return channel.trim().toUpperCase(Locale.ROOT);
    }
}
