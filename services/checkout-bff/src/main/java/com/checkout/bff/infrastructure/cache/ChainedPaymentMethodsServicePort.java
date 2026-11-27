package com.checkout.bff.infrastructure.cache;

import com.checkout.bff.application.port.PaymentMethodsServicePort;
import com.checkout.bff.domain.entity.PaymentMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Chained payment methods service port that delegates to the payment-service REST client.
 * Caching is handled by the payment-service itself (via @Cacheable on findAllActive).
 * The BFF filters the results locally as needed.
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class ChainedPaymentMethodsServicePort implements PaymentMethodsServicePort {

    private final PaymentMethodsServicePort restClient;

    @Override
    public Optional<PaymentMethod> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        // Delegate to payment-service (which handles caching internally)
        log.debug("Fetching payment method by name from payment-service: {}", name);
        return restClient.findByName(name);
    }

    @Override
    public List<PaymentMethod> findAllActive() {
        // Delegate to payment-service (which handles caching internally via @Cacheable)
        log.debug("Fetching all active payment methods from payment-service");
        return restClient.findAllActive();
    }

    @Override
    public Optional<PaymentMethod> findByNormalizedName(String normalizedName) {
        if (normalizedName == null || normalizedName.isBlank()) {
            return Optional.empty();
        }

        // Delegate to payment-service (which handles caching internally)
        log.debug("Fetching payment method by normalized name from payment-service: {}", normalizedName);
        return restClient.findByNormalizedName(normalizedName);
    }
}