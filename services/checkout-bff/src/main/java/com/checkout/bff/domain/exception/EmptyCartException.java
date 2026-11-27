package com.checkout.bff.domain.exception;

public class EmptyCartException extends RuntimeException {

    public EmptyCartException() {
        super("At least one product is required");
    }
}