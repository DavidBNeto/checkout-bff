package com.checkout.bff.infrastructure.client;

import com.checkout.bff.application.port.PaymentMethodsServicePort;
import com.checkout.bff.domain.entity.PaymentMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment-methods-service.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentMethodsServiceClient implements PaymentMethodsServicePort {

    private final RestClient paymentMethodsServiceRestClient;

    @Override
    public Optional<PaymentMethod> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        log.debug("Fetching payment method from Payment Methods Service: {}", name);

        try {
            PaymentMethod method = paymentMethodsServiceRestClient.get()
                    .uri("/api/v1/payment-methods/{name}", name)
                    .retrieve()
                    .onStatus(status -> status == HttpStatus.NOT_FOUND, (request, response) -> {
                        throw new PaymentMethodsServiceClientException("Payment method not found: " + name, response.getStatusCode());
                    })
                    .body(PaymentMethod.class);

            return Optional.ofNullable(method);
        } catch (HttpClientErrorException.NotFound e) {
            log.debug("Payment method not found in Payment Methods Service: {}", name);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error fetching payment method from Payment Methods Service: {}", name, e);
            throw new PaymentMethodsServiceClientException("Failed to fetch payment method: " + name, e);
        }
    }

    @Override
    public Optional<PaymentMethod> findByNormalizedName(String normalizedName) {
        if (normalizedName == null || normalizedName.isBlank()) {
            return Optional.empty();
        }

        log.debug("Fetching payment method from Payment Methods Service by normalized name: {}", normalizedName);

        try {
            PaymentMethod method = paymentMethodsServiceRestClient.get()
                    .uri("/api/v1/payment-methods/{normalizedName}", normalizedName)
                    .retrieve()
                    .onStatus(status -> status == HttpStatus.NOT_FOUND, (request, response) -> {
                        throw new PaymentMethodsServiceClientException("Payment method not found for type: " + normalizedName, response.getStatusCode());
                    })
                    .body(PaymentMethod.class);

            return Optional.ofNullable(method);
        } catch (HttpClientErrorException.NotFound e) {
            log.debug("Payment method not found in Payment Methods Service for type: {}", normalizedName);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error fetching payment method from Payment Methods Service for type: {}", normalizedName, e);
            throw new PaymentMethodsServiceClientException("Failed to fetch payment method for type: " + normalizedName, e);
        }
    }

    @Override
    public List<PaymentMethod> findAllActive() {
        log.debug("Fetching active payment methods from Payment Methods Service");

        try {
            PaymentMethod[] paymentMethods = paymentMethodsServiceRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/payment-methods")
                            .queryParam("active", true)
                            .build())
                    .retrieve()
                    .onStatus(status -> status == HttpStatus.NOT_FOUND, (request, response) -> {
                        throw new PaymentMethodsServiceClientException("Payment methods not found", response.getStatusCode());
                    })
                    .body(PaymentMethod[].class);

            if (paymentMethods == null) {
                return Collections.emptyList();
            }

            return List.of(paymentMethods);
        } catch (HttpClientErrorException.NotFound e) {
            log.debug("No active payment methods found in Payment Methods Service");
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error fetching active payment methods from Payment Methods Service", e);
            throw new PaymentMethodsServiceClientException("Failed to fetch active payment methods", e);
        }
    }

    public static class PaymentMethodsServiceClientException extends RuntimeException {
        public PaymentMethodsServiceClientException(String message) {
            super(message);
        }

        public PaymentMethodsServiceClientException(String message, Throwable cause) {
            super(message, cause);
        }

        public PaymentMethodsServiceClientException(String message, org.springframework.http.HttpStatus status) {
            super(message + " (status: " + status + ")");
        }

        public PaymentMethodsServiceClientException(String message, org.springframework.http.HttpStatusCode status) {
            super(message + " (status: " + status + ")");
        }
    }
}