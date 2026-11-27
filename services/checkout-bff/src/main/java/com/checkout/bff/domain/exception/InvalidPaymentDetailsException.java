package com.checkout.bff.domain.exception;

public class InvalidPaymentDetailsException extends RuntimeException {

    private final String cartId;
    private final String reason;

    public InvalidPaymentDetailsException(String cartId, String reason) {
        super("Invalid payment details for cart " + cartId + ": " + reason);
        this.cartId = cartId;
        this.reason = reason;
    }

    public String getCartId() {
        return cartId;
    }

    public String getReason() {
        return reason;
    }
}