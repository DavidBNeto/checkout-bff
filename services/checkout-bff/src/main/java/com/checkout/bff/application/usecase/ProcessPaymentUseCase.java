package com.checkout.bff.application.usecase;

import com.checkout.bff.infrastructure.messaging.event.PaymentProcessedEvent;

public interface ProcessPaymentUseCase {

    /**
     * Checks if a payment has already been processed for the given checkoutId.
     */
    boolean isAlreadyProcessed(String checkoutId);

    /**
     * Processes the payment request event.
     */
    void execute(PaymentProcessedEvent event);
}