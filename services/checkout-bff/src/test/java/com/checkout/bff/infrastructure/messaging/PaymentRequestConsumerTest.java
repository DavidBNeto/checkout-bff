package com.checkout.bff.infrastructure.messaging;

import com.checkout.bff.application.usecase.ProcessPaymentUseCase;
import com.checkout.bff.infrastructure.messaging.event.PaymentProcessedEvent;
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
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentRequestConsumerTest {

    @Mock
    private ProcessPaymentUseCase processPaymentUseCase;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @Captor
    private ArgumentCaptor<PaymentProcessedEvent> eventCaptor;

    private PaymentRequestConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new PaymentRequestConsumer(processPaymentUseCase, objectMapper, rabbitTemplate);
    }

    @Nested
    @DisplayName("consume")
    class Consume {

        @Test
        @DisplayName("Should process payment request successfully")
        void shouldProcessPaymentRequestSuccessfully() throws Exception {
            // Given
            String checkoutId = "checkout-123";
            PaymentProcessedEvent event = PaymentProcessedEvent.builder()
                    .eventId("event-123")
                    .checkoutId(checkoutId)
                    .cartId("cart-123")
                    .status("PENDING")
                    .paymentMethodName("CREDIT_CARD")
                    .amount(BigDecimal.valueOf(100.00))
                    .currency("BRL")
                    .timestamp(Instant.now())
                    .build();

            Message message = new Message(
                    "{}".getBytes(),
                    new MessageProperties()
            );

            when(objectMapper.readValue(anyString(), eq(PaymentProcessedEvent.class))).thenReturn(event);
            when(processPaymentUseCase.isAlreadyProcessed(checkoutId)).thenReturn(false);

            // When
            consumer.consume(message);

            // Then
            verify(objectMapper).readValue(anyString(), eq(PaymentProcessedEvent.class));
            verify(processPaymentUseCase).isAlreadyProcessed(checkoutId);
            verify(processPaymentUseCase).execute(event);
        }

        @Test
        @DisplayName("Should skip processing when already processed")
        void shouldSkipProcessingWhenAlreadyProcessed() throws Exception {
            // Given
            String checkoutId = "checkout-123";
            PaymentProcessedEvent event = PaymentProcessedEvent.builder()
                    .eventId("event-123")
                    .checkoutId(checkoutId)
                    .cartId("cart-123")
                    .status("PENDING")
                    .paymentMethodName("CREDIT_CARD")
                    .amount(BigDecimal.valueOf(100.00))
                    .currency("BRL")
                    .timestamp(Instant.now())
                    .build();

            Message message = new Message(
                    "{}".getBytes(),
                    new MessageProperties()
            );

            when(objectMapper.readValue(anyString(), eq(PaymentProcessedEvent.class))).thenReturn(event);
            when(processPaymentUseCase.isAlreadyProcessed(checkoutId)).thenReturn(true);

            // When
            consumer.consume(message);

            // Then
            verify(objectMapper).readValue(anyString(), eq(PaymentProcessedEvent.class));
            verify(processPaymentUseCase).isAlreadyProcessed(checkoutId);
            verify(processPaymentUseCase, never()).execute(any(PaymentProcessedEvent.class));
        }

        @Test
        @DisplayName("Should throw exception when deserialization fails")
        void shouldThrowExceptionWhenDeserializationFails() throws Exception {
            // Given
            Message message = new Message(
                    "invalid json".getBytes(),
                    new MessageProperties()
            );

            when(objectMapper.readValue(any(byte[].class), eq(PaymentProcessedEvent.class)))
                    .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("Invalid JSON") {});

            // When/Then
            assertThatThrownBy(() -> consumer.consume(message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Payment processing failed");
        }

        @Test
        @DisplayName("Should throw exception when processing fails")
        void shouldThrowExceptionWhenProcessingFails() throws Exception {
            // Given
            String checkoutId = "checkout-123";
            PaymentProcessedEvent event = PaymentProcessedEvent.builder()
                    .eventId("event-123")
                    .checkoutId(checkoutId)
                    .cartId("cart-123")
                    .status("PENDING")
                    .paymentMethodName("CREDIT_CARD")
                    .amount(BigDecimal.valueOf(100.00))
                    .currency("BRL")
                    .timestamp(Instant.now())
                    .build();

            Message message = new Message(
                    "{}".getBytes(),
                    new MessageProperties()
            );

            when(objectMapper.readValue(anyString(), eq(PaymentProcessedEvent.class))).thenReturn(event);
            when(processPaymentUseCase.isAlreadyProcessed(checkoutId)).thenReturn(false);
            doThrow(new RuntimeException("Processing failed")).when(processPaymentUseCase).execute(event);

            // When/Then
            assertThatThrownBy(() -> consumer.consume(message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Payment processing failed");
        }
    }
}