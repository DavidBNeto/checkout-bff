package com.checkout.product.controller;

import com.checkout.product.domain.entity.Product;
import com.checkout.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<Product>> getProducts(
            @RequestParam(required = false) String slugs,
            @RequestParam(required = false, defaultValue = "false") boolean active) {

        log.debug("GET /api/v1/products - slugs: {}, active: {}", slugs, active);

        if (slugs != null && !slugs.isBlank()) {
            List<String> slugList = List.of(slugs.split(","));
            List<Product> products = productService.findBySlugInAndActiveTrue(slugList);
            return ResponseEntity.ok(products);
        }

        if (active) {
            List<Product> products = productService.findAllActive();
            return ResponseEntity.ok(products);
        }

        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/{slug}")
    public ResponseEntity<Product> getProductBySlug(@PathVariable String slug) {
        log.debug("GET /api/v1/products/{}", slug);
        return productService.findBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}