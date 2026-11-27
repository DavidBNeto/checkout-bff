package com.checkout.bff.domain.exception;

public class ProductNotFoundException extends RuntimeException {

    private final String slug;

    public ProductNotFoundException(String slug) {
        super("Product not found: " + slug);
        this.slug = slug;
    }

    public String getSlug() {
        return slug;
    }
}