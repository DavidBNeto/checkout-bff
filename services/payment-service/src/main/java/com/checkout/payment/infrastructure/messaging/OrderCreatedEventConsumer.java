package com.checkout.payment.infrastructure.messaging;

import com.checkout.payment.application.service.ProcessPaymentService;
import com.checkout.payment.infrastructure.messaging.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedEventConsumer {

    private final ProcessPaymentService processPaymentService;

    @RabbitListener(queues = "checkout.order.created", containerFactory = "rabbitListenerContainerFactory")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for checkout: {}", event.getCheckoutId());
        processPaymentService.processPayment(event);
    }
}