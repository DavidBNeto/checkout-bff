package com.checkout.bff.infrastructure.messaging;

import com.checkout.bff.infrastructure.messaging.event.CartUpdatedEvent;
import com.checkout.bff.infrastructure.messaging.event.OrderCreatedEvent;
import com.checkout.bff.infrastructure.messaging.event.PaymentProcessedEvent;
import com.checkout.bff.infrastructure.persistence.entity.OutboxEvent;
import com.checkout.bff.infrastructure.persistence.jpa.OutboxEventJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxMessagePublisher implements MessagePublisherPort {

    private final OutboxEventJpaRepository outboxRepository;
    @Qualifier("messagingObjectMapper")
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void publishCartUpdated(CartUpdatedEvent event) {
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID().toString());
        }
        if (event.getEventType() == null) {
            event.setEventType("CART_UPDATED");
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(java.time.Instant.now());
        }

        JsonNode payload = objectMapper.valueToTree(event);

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .payload(payload)
                .routingKey("cart.updated")
                .exchange("checkout.exchange")
                .createdAt(LocalDateTime.now())
                .build();

        outboxRepository.save(outboxEvent);
        log.debug("Saved CartUpdatedEvent to outbox: {}", event.getEventId());
    }

    @Override
    @Transactional
    public void publishOrderCreated(OrderCreatedEvent event) {
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID().toString());
        }
        if (event.getEventType() == null) {
            event.setEventType("ORDER_CREATED");
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(java.time.Instant.now());
        }

        JsonNode payload = objectMapper.valueToTree(event);

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .payload(payload)
                .routingKey("order.created")
                .exchange("checkout.exchange")
                .createdAt(LocalDateTime.now())
                .build();

        outboxRepository.save(outboxEvent);
        log.debug("Saved OrderCreatedEvent to outbox: {}", event.getEventId());
    }

    @Override
    @Transactional
    public void publishPaymentProcessed(PaymentProcessedEvent event) {
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID().toString());
        }
        if (event.getEventType() == null) {
            event.setEventType("PAYMENT_PROCESSED");
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(java.time.Instant.now());
        }

        JsonNode payload = objectMapper.valueToTree(event);

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .payload(payload)
                .routingKey("payment.request")
                .exchange("checkout.exchange")
                .createdAt(LocalDateTime.now())
                .build();

        outboxRepository.save(outboxEvent);
        log.debug("Saved PaymentProcessedEvent to outbox: {}", event.getEventId());
    }
}