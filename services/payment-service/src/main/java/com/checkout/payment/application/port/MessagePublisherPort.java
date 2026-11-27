package com.checkout.payment.application.port;

import com.checkout.payment.infrastructure.messaging.event.PaymentProcessedEvent;

public interface MessagePublisherPort {

    void publishPaymentProcessed(PaymentProcessedEvent event);
}