package com.checkout.bff.application.service;

import com.checkout.bff.domain.entity.CheckoutOperation;
import com.checkout.bff.domain.entity.ShoppingCart;
import com.checkout.bff.domain.repository.CheckoutOperationRepository;
import com.checkout.bff.domain.repository.ShoppingCartRepository;
import com.checkout.bff.infrastructure.messaging.event.PaymentProcessedEvent;
import com.checkout.bff.infrastructure.messaging.event.PaymentResultEvent;
import com.checkout.bff.infrastructure.redis.ProcessedEventStore;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessPaymentServiceTest {

    @Mock
    private CheckoutOperationRepository checkoutOperationRepository;
    @Mock
    private ShoppingCartRepository shoppingCartRepository;
    @Mock
    private ProcessedEventStore processedEventStore;

    @Captor
    private ArgumentCaptor<CheckoutOperation> operationCaptor;

    private ProcessPaymentService processPaymentService;

    @BeforeEach
    void setUp() {
        processPaymentService = new ProcessPaymentService(
                checkoutOperationRepository,
                shoppingCartRepository,
                processedEventStore
        );
    }

    @Nested
    @DisplayName("isAlreadyProcessed")
    class IsAlreadyProcessed {

        @Test
        @DisplayName("Should return true when event already processed")
        void shouldReturnTrueWhenEventAlreadyProcessed() {
            // Given
            String checkoutId = UUID.randomUUID().toString();
            when(processedEventStore.isProcessed(checkoutId)).thenReturn(true);

            // When
            boolean result = processPaymentService.isAlreadyProcessed(checkoutId);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when event not processed")
        void shouldReturnFalseWhenEventNotProcessed() {
            // Given
            String checkoutId = UUID.randomUUID().toString();
            when(processedEventStore.isProcessed(checkoutId)).thenReturn(false);

            // When
            boolean result = processPaymentService.isAlreadyProcessed(checkoutId);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Handle PaymentProcessedEvent from payment service")
    class HandlePaymentProcessedEvent {

        @Test
        @DisplayName("Should update checkout to APPROVED and mark cart PAID for approved payment")
        void shouldUpdateCheckoutToApprovedAndMarkCartPaidForApprovedPayment() {
            // Given
            String checkoutId = "123e4567-e89b-12d3-a456-426614174000";
            UUID checkoutUuid = UUID.fromString(checkoutId);

            CheckoutOperation operation = createCheckoutOperation(checkoutUuid, "CREDIT_CARD");
            ShoppingCart cart = operation.getCart();
            when(checkoutOperationRepository.findByPublicId(checkoutUuid)).thenReturn(Optional.of(operation));
            when(processedEventStore.isProcessed(checkoutId)).thenReturn(false);
            when(shoppingCartRepository.findById(cart.getId())).thenReturn(Optional.of(cart));

            PaymentResultEvent result = PaymentResultEvent.builder()
                    .authorizationCode("AUTH123456")
                    .maskedCardNumber("****1234")
                    .build();

            PaymentProcessedEvent event = PaymentProcessedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("PAYMENT_PROCESSED")
                    .checkoutId(checkoutId)
                    .cartId(cart.getPublicId().toString())
                    .status("APPROVED")
                    .paymentMethodName("Cartão de Crédito")
                    .amount(BigDecimal.valueOf(100.00))
                    .currency("BRL")
                    .result(result)
                    .timestamp(Instant.now())
                    .build();

            // When
            processPaymentService.execute(event);

            // Then - save is called once with updated operation
            verify(checkoutOperationRepository).save(operationCaptor.capture());
            verify(shoppingCartRepository).save(argThat(c -> "PAID".equals(c.getStatus())));
            verify(processedEventStore).markProcessed(eq(checkoutId), eq(checkoutId));

            CheckoutOperation savedOperation = operationCaptor.getValue();
            assertThat(savedOperation.getStatus()).isEqualTo("APPROVED");
            assertThat(savedOperation.getAuthorizationCode()).isEqualTo("AUTH123456");
            assertThat(savedOperation.getMaskedCardNumber()).isEqualTo("****1234");
        }

        @Test
        @DisplayName("Should update checkout to DECLINED and not mark cart PAID for declined payment")
        void shouldUpdateCheckoutToDeclinedAndNotMarkCartPaidForDeclinedPayment() {
            // Given
            String checkoutId = "123e4567-e89b-12d3-a456-426614174002";
            UUID checkoutUuid = UUID.fromString(checkoutId);

            CheckoutOperation operation = createCheckoutOperation(checkoutUuid, "CREDIT_CARD");
            when(checkoutOperationRepository.findByPublicId(checkoutUuid)).thenReturn(Optional.of(operation));
            when(processedEventStore.isProcessed(checkoutId)).thenReturn(false);

            PaymentResultEvent result = PaymentResultEvent.builder()
                    .failureReason("Payment declined by issuer")
                    .build();

            PaymentProcessedEvent event = PaymentProcessedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("PAYMENT_PROCESSED")
                    .checkoutId(checkoutId)
                    .cartId(UUID.randomUUID().toString())
                    .status("DECLINED")
                    .paymentMethodName("Cartão de Crédito")
                    .amount(BigDecimal.valueOf(100.00))
                    .currency("BRL")
                    .result(result)
                    .timestamp(Instant.now())
                    .build();

            // When
            processPaymentService.execute(event);

            // Then - save is called once with updated operation
            verify(checkoutOperationRepository).save(operationCaptor.capture());
            verify(shoppingCartRepository, never()).save(any());
            verify(processedEventStore).markProcessed(eq(checkoutId), eq(checkoutId));

            CheckoutOperation savedOperation = operationCaptor.getValue();
            assertThat(savedOperation.getStatus()).isEqualTo("DECLINED");
            assertThat(savedOperation.getFailureReason()).isEqualTo("Payment declined by issuer");
        }

        @Test
        @DisplayName("Should update checkout to APPROVED with PIX code for PIX payment")
        void shouldUpdateCheckoutToApprovedWithPixCodeForPixPayment() {
            // Given
            String checkoutId = UUID.randomUUID().toString();
            UUID checkoutUuid = UUID.fromString(checkoutId);

            CheckoutOperation operation = createCheckoutOperation(checkoutUuid, "PIX");
            ShoppingCart cart = operation.getCart();
            when(checkoutOperationRepository.findByPublicId(checkoutUuid)).thenReturn(Optional.of(operation));
            when(processedEventStore.isProcessed(checkoutId)).thenReturn(false);
            when(shoppingCartRepository.findById(cart.getId())).thenReturn(Optional.of(cart));

            PaymentResultEvent result = PaymentResultEvent.builder()
                    .pixCode("PIX|abc123")
                    .build();

            PaymentProcessedEvent event = PaymentProcessedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("PAYMENT_PROCESSED")
                    .checkoutId(checkoutId)
                    .cartId(cart.getPublicId().toString())
                    .status("APPROVED")
                    .paymentMethodName("PIX")
                    .amount(BigDecimal.valueOf(100.00))
                    .currency("BRL")
                    .result(result)
                    .timestamp(Instant.now())
                    .build();

            // When
            processPaymentService.execute(event);

            // Then - save is called once with updated operation
            verify(checkoutOperationRepository).save(operationCaptor.capture());
            verify(shoppingCartRepository).save(argThat(c -> "PAID".equals(c.getStatus())));
            verify(processedEventStore).markProcessed(eq(checkoutId), eq(checkoutId));

            CheckoutOperation savedOperation = operationCaptor.getValue();
            assertThat(savedOperation.getStatus()).isEqualTo("APPROVED");
            assertThat(savedOperation.getPixCode()).isEqualTo("PIX|abc123");
        }

        @Test
        @DisplayName("Should return early when checkout operation not found")
        void shouldReturnEarlyWhenCheckoutOperationNotFound() {
            // Given
            String checkoutId = UUID.randomUUID().toString();
            UUID checkoutUuid = UUID.fromString(checkoutId);

            when(processedEventStore.isProcessed(checkoutId)).thenReturn(false);
            when(checkoutOperationRepository.findByPublicId(checkoutUuid)).thenReturn(Optional.empty());

            // When
            PaymentProcessedEvent event = createPaymentProcessedEvent(checkoutId, "APPROVED");
            processPaymentService.execute(event);

            // Then
            verify(processedEventStore).isProcessed(checkoutId);
            verify(checkoutOperationRepository).findByPublicId(checkoutUuid);
            verifyNoMoreInteractions(checkoutOperationRepository);
            verifyNoInteractions(shoppingCartRepository);
            verifyNoMoreInteractions(processedEventStore);
        }

        @Test
        @DisplayName("Should return early when operation already in terminal state")
        void shouldReturnEarlyWhenOperationAlreadyInTerminalState() {
            // Given
            String checkoutId = UUID.randomUUID().toString();
            UUID checkoutUuid = UUID.fromString(checkoutId);

            CheckoutOperation operation = createCheckoutOperation(checkoutUuid, "CREDIT_CARD");
            operation.setStatus("APPROVED"); // Already terminal

            when(processedEventStore.isProcessed(checkoutId)).thenReturn(false);
            when(checkoutOperationRepository.findByPublicId(checkoutUuid)).thenReturn(Optional.of(operation));

            // When
            PaymentProcessedEvent event = createPaymentProcessedEvent(checkoutId, "APPROVED");
            processPaymentService.execute(event);

            // Then
            verify(processedEventStore).isProcessed(checkoutId);
            verify(checkoutOperationRepository).findByPublicId(checkoutUuid);
            verifyNoMoreInteractions(checkoutOperationRepository);
            verifyNoInteractions(shoppingCartRepository);
            verifyNoMoreInteractions(processedEventStore);
        }

        @Test
        @DisplayName("Should return early when already processed")
        void shouldReturnEarlyWhenAlreadyProcessed() {
            // Given
            String checkoutId = UUID.randomUUID().toString();

            when(processedEventStore.isProcessed(checkoutId)).thenReturn(true);

            // When
            PaymentProcessedEvent event = createPaymentProcessedEvent(checkoutId, "APPROVED");
            processPaymentService.execute(event);

            // Then
            verify(processedEventStore).isProcessed(checkoutId);
            verifyNoMoreInteractions(processedEventStore);
            verifyNoInteractions(checkoutOperationRepository);
            verifyNoInteractions(shoppingCartRepository);
        }
    }

    // Helper methods
    private CheckoutOperation createCheckoutOperation(UUID checkoutId, String paymentMethodType) {
        ShoppingCart cart = new ShoppingCart();
        cart.setId(1L);
        cart.setPublicId(UUID.randomUUID());
        cart.setStatus("PENDING");

        CheckoutOperation operation = CheckoutOperation.builder()
                .cart(cart)
                .status("PENDING")
                .paymentMethodType(paymentMethodType)
                .paymentMethodName("Test Payment")
                .amount(BigDecimal.valueOf(100.00))
                .currency("BRL")
                .build();
        operation.setPublicId(checkoutId);
        return operation;
    }

    private PaymentProcessedEvent createPaymentProcessedEvent(String checkoutId, String status) {
        return PaymentProcessedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PAYMENT_PROCESSED")
                .checkoutId(checkoutId)
                .cartId(UUID.randomUUID().toString())
                .status(status)
                .paymentMethodName("Test Payment")
                .amount(BigDecimal.valueOf(100.00))
                .currency("BRL")
                .result(PaymentResultEvent.builder().build())
                .timestamp(Instant.now())
                .build();
    }
}