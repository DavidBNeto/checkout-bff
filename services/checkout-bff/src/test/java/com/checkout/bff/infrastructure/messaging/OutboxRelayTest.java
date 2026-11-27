package com.checkout.bff.infrastructure.messaging;

import com.checkout.bff.infrastructure.persistence.entity.OutboxEvent;
import com.checkout.bff.infrastructure.persistence.jpa.OutboxEventJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    private OutboxEventJpaRepository outboxRepository;

    @Mock
    private AmqpTemplate amqpTemplate;

    @Captor
    private ArgumentCaptor<Long> idCaptor;

    @Captor
    private ArgumentCaptor<LocalDateTime> timestampCaptor;

    @Captor
    private ArgumentCaptor<String> errorMessageCaptor;

    private OutboxRelay outboxRelay;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        outboxRelay = new OutboxRelay(outboxRepository, amqpTemplate, objectMapper);
        // Use reflection to set private fields
        try {
            java.lang.reflect.Field enabledField = OutboxRelay.class.getDeclaredField("enabled");
            enabledField.setAccessible(true);
            enabledField.set(outboxRelay, true);

            java.lang.reflect.Field batchSizeField = OutboxRelay.class.getDeclaredField("batchSize");
            batchSizeField.setAccessible(true);
            batchSizeField.set(outboxRelay, 100);

            java.lang.reflect.Field maxRetriesField = OutboxRelay.class.getDeclaredField("maxRetries");
            maxRetriesField.setAccessible(true);
            maxRetriesField.set(outboxRelay, 5);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("publishUnpublishedEvents")
    class PublishUnpublishedEvents {

        @Test
        @DisplayName("Should do nothing when disabled")
        void shouldDoNothingWhenDisabled() {
            // Given
            try {
                java.lang.reflect.Field enabledField = OutboxRelay.class.getDeclaredField("enabled");
                enabledField.setAccessible(true);
                enabledField.set(outboxRelay, false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // When
            outboxRelay.publishUnpublishedEvents();

            // Then
            verifyNoInteractions(outboxRepository);
            verifyNoInteractions(amqpTemplate);
        }

        @Test
        @DisplayName("Should do nothing when no unpublished events")
        void shouldDoNothingWhenNoUnpublishedEvents() {
            // Given
            when(outboxRepository.findFirst100UnpublishedEvents()).thenReturn(List.of());

            // When
            outboxRelay.publishUnpublishedEvents();

            // Then
            verify(outboxRepository).findFirst100UnpublishedEvents();
            verifyNoInteractions(amqpTemplate);
        }

        @Test
        @DisplayName("Should publish events successfully")
        void shouldPublishEventsSuccessfully() {
            // Given
            JsonNode payload = objectMapper.createObjectNode();
            OutboxEvent event = createOutboxEvent(1L, "event-1", "cart.updated", "checkout.exchange", payload);
            when(outboxRepository.findFirst100UnpublishedEvents()).thenReturn(List.of(event));

            // When
            outboxRelay.publishUnpublishedEvents();

            // Then
            verify(amqpTemplate).send(eq("checkout.exchange"), eq("cart.updated"), any(Message.class));
            verify(outboxRepository).markAsPublished(eq(1L), any(LocalDateTime.class));
            verify(outboxRepository, never()).markAsFailed(anyLong(), any(LocalDateTime.class), anyString());
        }

        @Test
        @DisplayName("Should handle multiple events")
        void shouldHandleMultipleEvents() {
            // Given
            JsonNode payload = objectMapper.createObjectNode();
            OutboxEvent event1 = createOutboxEvent(1L, "event-1", "cart.updated", "checkout.exchange", payload);
            OutboxEvent event2 = createOutboxEvent(2L, "event-2", "order.created", "checkout.exchange", payload);
            when(outboxRepository.findFirst100UnpublishedEvents()).thenReturn(List.of(event1, event2));

            // When
            outboxRelay.publishUnpublishedEvents();

            // Then
            verify(amqpTemplate).send(eq("checkout.exchange"), eq("cart.updated"), any(Message.class));
            verify(amqpTemplate).send(eq("checkout.exchange"), eq("order.created"), any(Message.class));
            verify(outboxRepository).markAsPublished(eq(1L), any(LocalDateTime.class));
            verify(outboxRepository).markAsPublished(eq(2L), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should mark as failed when publish fails")
        void shouldMarkAsFailedWhenPublishFails() {
            // Given
            JsonNode payload = objectMapper.createObjectNode();
            OutboxEvent event = createOutboxEvent(1L, "event-1", "cart.updated", "checkout.exchange", payload);
            when(outboxRepository.findFirst100UnpublishedEvents()).thenReturn(List.of(event));
            doThrow(new RuntimeException("Connection refused")).when(amqpTemplate).send(anyString(), anyString(), any(Message.class));

            // When
            outboxRelay.publishUnpublishedEvents();

            // Then
            verify(amqpTemplate).send(eq("checkout.exchange"), eq("cart.updated"), any(Message.class));
            verify(outboxRepository, never()).markAsPublished(anyLong(), any(LocalDateTime.class));
            verify(outboxRepository).markAsFailed(eq(1L), any(LocalDateTime.class), contains("Connection refused"));
        }

        @Test
        @DisplayName("Should continue with other events when one fails")
        void shouldContinueWithOtherEventsWhenOneFails() {
            // Given
            JsonNode payload = objectMapper.createObjectNode();
            OutboxEvent event1 = createOutboxEvent(1L, "event-1", "cart.updated", "checkout.exchange", payload);
            OutboxEvent event2 = createOutboxEvent(2L, "event-2", "order.created", "checkout.exchange", payload);
            when(outboxRepository.findFirst100UnpublishedEvents()).thenReturn(List.of(event1, event2));
            doThrow(new RuntimeException("Connection refused")).when(amqpTemplate).send(eq("checkout.exchange"), eq("cart.updated"), any(Message.class));
            doNothing().when(amqpTemplate).send(eq("checkout.exchange"), eq("order.created"), any(Message.class));

            // When
            outboxRelay.publishUnpublishedEvents();

            // Then
            verify(outboxRepository).markAsFailed(eq(1L), any(LocalDateTime.class), contains("Connection refused"));
            verify(outboxRepository).markAsPublished(eq(2L), any(LocalDateTime.class));
        }
    }

    private OutboxEvent createOutboxEvent(Long id, String eventId, String routingKey, String exchange, JsonNode payload) {
        return OutboxEvent.builder()
                .id(id)
                .eventId(eventId)
                .eventType("TEST_EVENT")
                .payload(payload)
                .routingKey(routingKey)
                .exchange(exchange)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .build();
    }

    private static String contains(String substring) {
        return org.mockito.ArgumentMatchers.argThat(s -> s instanceof String && ((String) s).contains(substring));
    }
}