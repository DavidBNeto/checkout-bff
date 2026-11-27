package com.checkout.bff.domain.exception;

public class PaymentDeclinedException extends RuntimeException {

    private final String cartId;
    private final String paymentMethod;

    public PaymentDeclinedException(String cartId, String paymentMethod, String reason) {
        super("Payment declined for cart " + cartId + " with " + paymentMethod + ": " + reason);
        this.cartId = cartId;
        this.paymentMethod = paymentMethod;
    }

    public String getCartId() {
        return cartId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }
}