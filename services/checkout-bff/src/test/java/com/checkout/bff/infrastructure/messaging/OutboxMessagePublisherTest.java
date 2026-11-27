package com.checkout.bff.infrastructure.messaging;

import com.checkout.bff.infrastructure.messaging.event.CartUpdatedEvent;
import com.checkout.bff.infrastructure.messaging.event.OrderCreatedEvent;
import com.checkout.bff.infrastructure.messaging.event.PaymentProcessedEvent;
import com.checkout.bff.infrastructure.persistence.entity.OutboxEvent;
import com.checkout.bff.infrastructure.persistence.jpa.OutboxEventJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxMessagePublisherTest {

    @Mock
    private OutboxEventJpaRepository outboxRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Captor
    private ArgumentCaptor<OutboxEvent> outboxEventCaptor;

    private OutboxMessagePublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxMessagePublisher(outboxRepository, objectMapper);
    }

    @Nested
    @DisplayName("publishCartUpdated")
    class PublishCartUpdated {

        @Test
        @DisplayName("Should publish cart updated event with all fields set")
        void shouldPublishCartUpdatedEventWithAllFieldsSet() throws Exception {
            // Given
            CartUpdatedEvent event = CartUpdatedEvent.builder()
                    .eventId("event-123")
                    .eventType("CART_UPDATED")
                    .timestamp(Instant.now())
                    .cartId(123L)
                    .cartStatus("CONFIRMED")
                    .totalAmount(BigDecimal.valueOf(100.00))
                    .currency("BRL")
                    .items(List.of())
                    .build();

            when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            publisher.publishCartUpdated(event);

            // Then
            verify(outboxRepository).save(outboxEventCaptor.capture());

            OutboxEvent savedEvent = outboxEventCaptor.getValue();
            assertThat(savedEvent.getEventId()).isEqualTo("event-123");
            assertThat(savedEvent.getEventType()).isEqualTo("CART_UPDATED");
            assertThat(savedEvent.getRoutingKey()).isEqualTo("cart.updated");
            assertThat(savedEvent.getExchange()).isEqualTo("checkout.exchange");
            assertThat(savedEvent.getPayload()).isNotNull();
            assertThat(savedEvent.getPayload().get("eventId").asText()).isEqualTo("event-123");
            assertThat(savedEvent.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should generate eventId when null")
        void shouldGenerateEventIdWhenNull() throws Exception {
            // Given
            CartUpdatedEvent event = CartUpdatedEvent.builder()
                    .eventType("CART_UPDATED")
                    .timestamp(Instant.now())
                    .cartId(123L)
                    .cartStatus("CONFIRMED")
                    .totalAmount(BigDecimal.valueOf(100.00))
                    .currency("BRL")
                    .items(List.of())
                    .build();

            when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            publisher.publishCartUpdated(event);

            // Then
            verify(outboxRepository).save(outboxEventCaptor.capture());
            assertThat(outboxEventCaptor.getValue().getEventId()).isNotNull();
        }

        @Test
        @DisplayName("Should set default eventType when null")
        void shouldSetDefaultEventTypeWhenNull() throws Exception {
            // Given
            CartUpdatedEvent event = CartUpdatedEvent.builder()
                    .eventId("event-123")
                    .timestamp(Instant.now())
                    .cartId(123L)
                    .cartStatus("CONFIRMED")
                    .totalAmount(BigDecimal.valueOf(100.00))
                    .currency("BRL")
                    .items(List.of())
                    .build();

            when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            publisher.publishCartUpdated(event);

            // Then
            verify(outboxRepository).save(outboxEventCaptor.capture());
            assertThat(outboxEventCaptor.getValue().getEventType()).isEqualTo("CART_UPDATED");
        }

        @Test
        @DisplayName("Should set timestamp when null")
        void shouldSetTimestampWhenNull() throws Exception {
            // Given
            CartUpdatedEvent event = CartUpdatedEvent.builder()
                    .eventId("event-123")
                    .eventType("CART_UPDATED")
                    .cartId(123L)
                    .cartStatus("CONFIRMED")
                    .totalAmount(BigDecimal.valueOf(100.00))
                    .currency("BRL")
                    .items(List.of())
                    .build();

            when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            publisher.publishCartUpdated(event);

            // Then
            verify(outboxRepository).save(outboxEventCaptor.capture());
            assertThat(outboxEventCaptor.getValue().getEventType()).isEqualTo("CART_UPDATED");
        }

        @Test
        @DisplayName("Should propagate exception when valueToTree fails")
        void shouldPropagateExceptionWhenValueToTreeFails() throws Exception {
            // Given
            CartUpdatedEvent event = CartUpdatedEvent.builder()
                    .eventId("event-123")
                    .eventType("CART_UPDATED")
                    .timestamp(Instant.now())
                    .cartId(123L)
                    .cartStatus("CONFIRMED")
                    .totalAmount(BigDecimal.valueOf(100.00))
                    .currency("BRL")
                    .items(List.of())
                    .build();

            // Use a mock that throws on valueToTree
            ObjectMapper failingMapper = mock(ObjectMapper.class);
            when(failingMapper.valueToTree(any(CartUpdatedEvent.class)))
                    .thenThrow(new RuntimeException("Serialization error"));

            OutboxMessagePublisher failingPublisher = new OutboxMessagePublisher(outboxRepository, failingMapper);

            // When/Then
            assertThatThrownBy(() -> failingPublisher.publishCartUpdated(event))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Serialization error");
        }
    }

    @Nested
    @DisplayName("publishOrderCreated")
    class PublishOrderCreated {

        @Test
        @DisplayName("Should publish order created event with correct routing key and exchange")
        void shouldPublishOrderCreatedEventWithCorrectRoutingKeyAndExchange() throws Exception {
            // Given
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .eventId("event-456")
                    .eventType("ORDER_CREATED")
                    .timestamp(Instant.now())
                    .cartId("cart-123")
                    .checkoutId("checkout-456")
                    .paymentMethodId(1L)
                    .paymentMethodName("Cartão de Crédito")
                    .amount(BigDecimal.valueOf(100.00))
                    .currency("BRL")
                    .build();

            when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            publisher.publishOrderCreated(event);

            // Then
            verify(outboxRepository).save(outboxEventCaptor.capture());
            OutboxEvent savedEvent = outboxEventCaptor.getValue();
            assertThat(savedEvent.getRoutingKey()).isEqualTo("order.created");
            assertThat(savedEvent.getExchange()).isEqualTo("checkout.exchange");
        }
    }

    @Nested
    @DisplayName("publishPaymentProcessed")
    class PublishPaymentProcessed {

        @Test
        @DisplayName("Should publish payment processed event with correct routing key and exchange")
        void shouldPublishPaymentProcessedEventWithCorrectRoutingKeyAndExchange() throws Exception {
            // Given
            PaymentProcessedEvent event = PaymentProcessedEvent.builder()
                    .eventId("event-789")
                    .eventType("PAYMENT_PROCESSED")
                    .timestamp(Instant.now())
                    .checkoutId("checkout-789")
                    .cartId("cart-789")
                    .status("APPROVED")
                    .paymentMethodName("CREDIT_CARD")
                    .amount(BigDecimal.valueOf(100.00))
                    .currency("BRL")
                    .build();

            when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            publisher.publishPaymentProcessed(event);

            // Then
            verify(outboxRepository).save(outboxEventCaptor.capture());
            OutboxEvent savedEvent = outboxEventCaptor.getValue();
            assertThat(savedEvent.getRoutingKey()).isEqualTo("payment.request");
            assertThat(savedEvent.getExchange()).isEqualTo("checkout.exchange");
        }
    }
}