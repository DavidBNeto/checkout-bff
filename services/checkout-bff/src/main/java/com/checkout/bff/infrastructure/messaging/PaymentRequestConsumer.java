package com.checkout.bff.infrastructure.messaging;

import com.checkout.bff.application.usecase.ProcessPaymentUseCase;
import com.checkout.bff.infrastructure.messaging.event.PaymentProcessedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRequestConsumer {

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = "${spring.rabbitmq.queue.payment.processed:checkout.payment.processed}")
    public void consume(Message message) throws IOException {
        try {
            String payload = new String(message.getBody());
            PaymentProcessedEvent event = objectMapper.readValue(payload, PaymentProcessedEvent.class);

            log.info("Received payment request for checkout: {}", event.getCheckoutId());

            // Idempotency check
            if (processPaymentUseCase.isAlreadyProcessed(event.getCheckoutId())) {
                log.info("Duplicate payment request for checkoutId: {}", event.getCheckoutId());
                return;
            }

            processPaymentUseCase.execute(event);

        } catch (Exception ex) {
            log.error("Failed to process payment request", ex);
            // Let RabbitMQ handle retries/DLQ via configuration
            throw new RuntimeException("Payment processing failed", ex);
        }
    }
}