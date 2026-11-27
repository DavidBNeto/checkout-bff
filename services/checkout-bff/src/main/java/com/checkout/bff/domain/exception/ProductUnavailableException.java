package com.checkout.bff.domain.exception;

public class ProductUnavailableException extends RuntimeException {

    private final String slug;

    public ProductUnavailableException(String slug) {
        super("Product unavailable: " + slug);
        this.slug = slug;
    }

    public String getSlug() {
        return slug;
    }
}