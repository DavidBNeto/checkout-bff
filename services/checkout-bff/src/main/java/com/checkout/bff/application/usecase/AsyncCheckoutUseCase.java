package com.checkout.bff.application.usecase;

import com.checkout.bff.application.dto.AsyncCheckoutResponse;
import com.checkout.bff.application.dto.CreateAsyncCheckoutCommand;

import java.util.UUID;

public interface AsyncCheckoutUseCase {

    /**
     * Initiates an asynchronous checkout operation.
     * Returns 202 Accepted with checkout details (status = PENDING).
     */
    AsyncCheckoutResponse initiateAsyncCheckout(CreateAsyncCheckoutCommand command);

    /**
     * Gets the current status of an async checkout operation.
     */
    AsyncCheckoutResponse getCheckoutStatus(UUID checkoutId);
}