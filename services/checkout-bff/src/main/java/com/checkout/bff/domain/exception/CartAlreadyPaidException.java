package com.checkout.bff.domain.exception;

public class CartAlreadyPaidException extends RuntimeException {

    private final String cartId;

    public CartAlreadyPaidException(String cartId) {
        super("Cart already paid: " + cartId);
        this.cartId = cartId;
    }

    public String getCartId() {
        return cartId;
    }
}