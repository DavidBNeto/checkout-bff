package com.checkout.payment.infrastructure.messaging;

import com.checkout.payment.infrastructure.persistence.entity.OutboxEvent;
import com.checkout.payment.infrastructure.persistence.jpa.OutboxEventJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxEventJpaRepository outboxRepository;
    private final AmqpTemplate amqpTemplate;
    @Qualifier("messagingObjectMapper")
    private final ObjectMapper objectMapper;

    @Value("${outbox.relay.batch-size:100}")
    private int batchSize;

    @Value("${outbox.relay.max-retries:5}")
    private int maxRetries;

    @Value("${outbox.relay.enabled:true}")
    private boolean enabled;

    @Scheduled(fixedDelayString = "${outbox.relay.poll-interval-ms:1000}")
    @Transactional
    public void publishUnpublishedEvents() {
        if (!enabled) {
            return;
        }

        List<OutboxEvent> events = outboxRepository.findFirst100UnpublishedEvents();
        if (events.isEmpty()) {
            return;
        }

        log.debug("Publishing {} unpublished outbox events", events.size());

        for (OutboxEvent event : events) {
            try {
                // Convert JsonNode to JSON string to avoid type header issues
                String jsonPayload = objectMapper.writeValueAsString(event.getPayload());

                // Create message with proper content type
                Message message = MessageBuilder.withBody(jsonPayload.getBytes(StandardCharsets.UTF_8))
                        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                        .build();

                amqpTemplate.send(event.getExchange(), event.getRoutingKey(), message);
                outboxRepository.markAsPublished(event.getId(), LocalDateTime.now());
                log.debug("Published outbox event: {} (id: {})", event.getEventId(), event.getId());
            } catch (Exception e) {
                log.error("Failed to publish outbox event: {} (id: {})", event.getEventId(), event.getId(), e);
                outboxRepository.markAsFailed(event.getId(), LocalDateTime.now(), e.getMessage());
            }
        }
    }
}