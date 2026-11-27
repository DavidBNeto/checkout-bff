package com.checkout.product.service;

import com.checkout.product.domain.entity.Product;
import com.checkout.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public Optional<Product> findBySlug(String slug) {
        log.debug("Fetching product by slug from database: {}", slug);
        return productRepository.findBySlug(slug);
    }

    public List<Product> findBySlugInAndActiveTrue(List<String> slugs) {
        log.debug("Fetching products by slugs from database: {}", slugs);
        return productRepository.findBySlugInAndActiveTrue(slugs);
    }

    @Cacheable(value = "products", keyGenerator = "productCacheKeyGenerator")
    public List<Product> findAllActive() {
        log.debug("Fetching all active products from database");
        return productRepository.findByActiveTrue();
    }
}