package com.checkout.payment.infrastructure.messaging;

import com.checkout.payment.application.port.MessagePublisherPort;
import com.checkout.payment.infrastructure.messaging.event.PaymentProcessedEvent;
import com.checkout.payment.infrastructure.persistence.entity.OutboxEvent;
import com.checkout.payment.infrastructure.persistence.jpa.OutboxEventJpaRepository;
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
                .routingKey("payment.processed")
                .exchange("checkout.exchange")
                .createdAt(LocalDateTime.now())
                .build();

        outboxRepository.save(outboxEvent);
        log.debug("Saved PaymentProcessedEvent to outbox: {}", event.getEventId());
    }
}