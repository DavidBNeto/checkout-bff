package com.checkout.bff.infrastructure.cache;

import com.checkout.bff.application.port.ProductServicePort;
import com.checkout.bff.domain.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Chained product service port that delegates to the product-service REST client.
 * Caching is handled by the product-service itself (via @Cacheable on findAllActive).
 * The BFF filters the results locally as needed.
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class ChainedProductServicePort implements ProductServicePort {

    private final ProductServicePort restClient;

    @Override
    public Optional<Product> findBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }

        // Delegate to product-service (which handles caching internally)
        log.debug("Fetching product by slug from product-service: {}", slug);
        return restClient.findBySlug(slug);
    }

    @Override
    public List<Product> findBySlugInAndActiveTrue(List<String> slugs) {
        if (slugs == null || slugs.isEmpty()) {
            return List.of();
        }

        // Fetch all active products from product-service (cached there) and filter locally
        log.debug("Fetching products by slugs from product-service: {}", slugs);
        List<Product> allActive = restClient.findAllActive();

        return allActive.stream()
                .filter(p -> slugs.contains(p.getSlug()))
                .toList();
    }

    @Override
    public List<Product> findAllActive() {
        // Delegate to product-service (which handles caching internally via @Cacheable)
        log.debug("Fetching all active products from product-service");
        return restClient.findAllActive();
    }
}