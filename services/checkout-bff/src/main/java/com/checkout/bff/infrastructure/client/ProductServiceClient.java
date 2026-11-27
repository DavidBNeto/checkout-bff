package com.checkout.bff.infrastructure.client;

import com.checkout.bff.application.port.ProductServicePort;
import com.checkout.bff.domain.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "product-service.enabled", havingValue = "true", matchIfMissing = true)
public class ProductServiceClient implements ProductServicePort {

    private final RestClient productServiceRestClient;

    @Override
    public Optional<Product> findBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }

        log.debug("Fetching product from Product Service: {}", slug);

        try {
            Product product = productServiceRestClient.get()
                    .uri("/api/v1/products/{slug}", slug)
                    .retrieve()
                    .onStatus(status -> status == HttpStatus.NOT_FOUND, (request, response) -> {
                        throw new ProductServiceClientException("Product not found: " + slug, response.getStatusCode());
                    })
                    .body(Product.class);

            return Optional.ofNullable(product);
        } catch (HttpClientErrorException.NotFound e) {
            log.debug("Product not found in Product Service: {}", slug);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error fetching product from Product Service: {}", slug, e);
            throw new ProductServiceClientException("Failed to fetch product: " + slug, e);
        }
    }

    @Override
    public List<Product> findBySlugInAndActiveTrue(List<String> slugs) {
        if (slugs == null || slugs.isEmpty()) {
            return Collections.emptyList();
        }

        log.debug("Fetching products from Product Service: {}", slugs);

        try {
            String slugsParam = String.join(",", slugs);
            Product[] products = productServiceRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/products")
                            .queryParam("slugs", slugsParam)
                            .queryParam("active", true)
                            .build())
                    .retrieve()
                    .onStatus(status -> status == HttpStatus.NOT_FOUND, (request, response) -> {
                        throw new ProductServiceClientException("Products not found: " + slugs, response.getStatusCode());
                    })
                    .body(Product[].class);

            if (products == null) {
                return Collections.emptyList();
            }

            return List.of(products);
        } catch (HttpClientErrorException.NotFound e) {
            log.debug("Products not found in Product Service: {}", slugs);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error fetching products from Product Service: {}", slugs, e);
            throw new ProductServiceClientException("Failed to fetch products: " + slugs, e);
        }
    }

    @Override
    public List<Product> findAllActive() {
        log.debug("Fetching all active products from Product Service");

        try {
            Product[] products = productServiceRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/products")
                            .queryParam("active", true)
                            .build())
                    .retrieve()
                    .body(Product[].class);

            if (products == null) {
                return Collections.emptyList();
            }

            return List.of(products);
        } catch (HttpClientErrorException.NotFound e) {
            log.debug("No active products found in Product Service");
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error fetching all active products from Product Service", e);
            throw new ProductServiceClientException("Failed to fetch all active products", e);
        }
    }

    public static class ProductServiceClientException extends RuntimeException {
        public ProductServiceClientException(String message) {
            super(message);
        }

        public ProductServiceClientException(String message, Throwable cause) {
            super(message, cause);
        }

        public ProductServiceClientException(String message, org.springframework.http.HttpStatus status) {
            super(message + " (status: " + status + ")");
        }

        public ProductServiceClientException(String message, org.springframework.http.HttpStatusCode status) {
            super(message + " (status: " + status + ")");
        }
    }
}