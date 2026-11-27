package com.checkout.bff.application.port;

import com.checkout.bff.domain.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductServicePort {

    Optional<Product> findBySlug(String slug);

    List<Product> findBySlugInAndActiveTrue(List<String> slugs);

    List<Product> findAllActive();
}