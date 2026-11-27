package com.checkout.bff.infrastructure.messaging;

import com.checkout.bff.infrastructure.messaging.event.CartUpdatedEvent;
import com.checkout.bff.infrastructure.messaging.event.OrderCreatedEvent;
import com.checkout.bff.infrastructure.messaging.event.PaymentProcessedEvent;

public interface MessagePublisherPort {

    void publishCartUpdated(CartUpdatedEvent event);

    void publishOrderCreated(OrderCreatedEvent event);

    void publishPaymentProcessed(PaymentProcessedEvent event);
}