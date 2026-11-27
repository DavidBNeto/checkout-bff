package com.checkout.bff.domain.exception;

public class CheckoutOperationNotFoundException extends RuntimeException {

    private final java.util.UUID checkoutId;

    public CheckoutOperationNotFoundException(java.util.UUID checkoutId) {
        super("Checkout operation not found: " + checkoutId);
        this.checkoutId = checkoutId;
    }

    public java.util.UUID getCheckoutId() {
        return checkoutId;
    }
}