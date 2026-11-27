package com.checkout.bff.application.service;

import com.checkout.bff.application.dto.AsyncCheckoutResponse;
import com.checkout.bff.application.dto.CreateAsyncCheckoutCommand;
import com.checkout.bff.application.mapper.AsyncCheckoutMapper;
import com.checkout.bff.application.port.PaymentMethodsServicePort;
import com.checkout.bff.domain.entity.CheckoutOperation;
import com.checkout.bff.domain.entity.PaymentMethod;
import com.checkout.bff.domain.entity.ShoppingCart;
import com.checkout.bff.domain.exception.CartAlreadyPaidException;
import com.checkout.bff.domain.exception.CartNotFoundException;
import com.checkout.bff.domain.exception.CheckoutOperationNotFoundException;
import com.checkout.bff.domain.exception.IdempotencyConflictException;
import com.checkout.bff.domain.exception.InvalidPaymentDetailsException;
import com.checkout.bff.domain.repository.CheckoutOperationRepository;
import com.checkout.bff.domain.repository.ShoppingCartRepository;
import com.checkout.bff.infrastructure.messaging.MessagePublisherPort;
import com.checkout.bff.infrastructure.messaging.event.OrderCreatedEvent;
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
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncCheckoutServiceTest {

    @Mock
    private ShoppingCartRepository shoppingCartRepository;
    @Mock
    private PaymentMethodsServicePort paymentMethodsService;
    @Mock
    private CheckoutOperationRepository checkoutOperationRepository;
    @Mock
    private AsyncCheckoutMapper asyncCheckoutMapper;
    @Mock
    private MessagePublisherPort messagePublisher;

    @Captor
    private ArgumentCaptor<OrderCreatedEvent> eventCaptor;

    private AsyncCheckoutService asyncCheckoutService;

    @BeforeEach
    void setUp() {
        asyncCheckoutService = new AsyncCheckoutService(
                shoppingCartRepository,
                paymentMethodsService,
                checkoutOperationRepository,
                asyncCheckoutMapper,
                messagePublisher
        );
    }

    @Nested
    @DisplayName("Initiate Async Checkout")
    class InitiateAsyncCheckout {

        @Test
        @DisplayName("Should initiate async checkout successfully with CREDIT_CARD")
        void shouldInitiateAsyncCheckoutSuccessfullyWithCreditCard() {
            // Given
            UUID cartId = UUID.randomUUID();
            String idempotencyKey = "idem-key-123";

            ShoppingCart cart = createShoppingCart(cartId, "PENDING");
            PaymentMethod paymentMethod = createPaymentMethod("Cartão de Crédito", "CREDIT_CARD", true);
            CheckoutOperation operation = createCheckoutOperation(cart, paymentMethod);
            AsyncCheckoutResponse expectedResponse = createAsyncCheckoutResponse(operation);

            when(shoppingCartRepository.findByPublicIdWithItems(cartId)).thenReturn(Optional.of(cart));
            when(paymentMethodsService.findByNormalizedName("CREDIT_CARD")).thenReturn(Optional.of(paymentMethod));
            when(checkoutOperationRepository.save(any(CheckoutOperation.class))).thenReturn(operation);
            when(asyncCheckoutMapper.toResponse(operation)).thenReturn(expectedResponse);

            CreateAsyncCheckoutCommand command = createCommandWithCard(cartId, "CREDIT_CARD", idempotencyKey);

            // When
            AsyncCheckoutResponse response = asyncCheckoutService.initiateAsyncCheckout(command);

            // Then
            assertThat(response).isEqualTo(expectedResponse);
            verify(checkoutOperationRepository).save(any(CheckoutOperation.class));
            verify(messagePublisher).publishOrderCreated(eventCaptor.capture());

            OrderCreatedEvent publishedEvent = eventCaptor.getValue();
            assertThat(publishedEvent.getCartId()).isEqualTo(cartId.toString());
            assertThat(publishedEvent.getEventType()).isEqualTo("ORDER_CREATED");
        }

        @Test
        @DisplayName("Should initiate async checkout successfully with PIX")
        void shouldInitiateAsyncCheckoutSuccessfullyWithPix() {
            // Given
            UUID cartId = UUID.randomUUID();
            String idempotencyKey = "idem-key-123";

            ShoppingCart cart = createShoppingCart(cartId, "PENDING");
            PaymentMethod paymentMethod = createPaymentMethod("PIX", "PIX", true);
            CheckoutOperation operation = createCheckoutOperation(cart, paymentMethod);
            AsyncCheckoutResponse expectedResponse = createAsyncCheckoutResponse(operation);

            when(shoppingCartRepository.findByPublicIdWithItems(cartId)).thenReturn(Optional.of(cart));
            when(paymentMethodsService.findByNormalizedName("PIX")).thenReturn(Optional.of(paymentMethod));
            when(checkoutOperationRepository.save(any(CheckoutOperation.class))).thenReturn(operation);
            when(asyncCheckoutMapper.toResponse(operation)).thenReturn(expectedResponse);

            CreateAsyncCheckoutCommand command = createCommandWithPixPayer(cartId, "PIX", idempotencyKey, "John Doe", "52998224725");

            // When
            AsyncCheckoutResponse response = asyncCheckoutService.initiateAsyncCheckout(command);

            // Then
            assertThat(response).isEqualTo(expectedResponse);
            verify(checkoutOperationRepository).save(any(CheckoutOperation.class));
            verify(messagePublisher).publishOrderCreated(eventCaptor.capture());

            OrderCreatedEvent publishedEvent = eventCaptor.getValue();
            assertThat(publishedEvent.getCartId()).isEqualTo(cartId.toString());
            assertThat(publishedEvent.getEventType()).isEqualTo("ORDER_CREATED");
        }

        @Test
        @DisplayName("Should throw CartNotFoundException when cart not found")
        void shouldThrowCartNotFoundExceptionWhenCartNotFound() {
            // Given
            UUID cartId = UUID.randomUUID();
            CreateAsyncCheckoutCommand command = createCommandWithCard(cartId, "CREDIT_CARD", "idem-key");

            when(checkoutOperationRepository.findByIdempotencyKey("idem-key")).thenReturn(Optional.empty());
            when(shoppingCartRepository.findByPublicIdWithItems(cartId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> asyncCheckoutService.initiateAsyncCheckout(command))
                    .isInstanceOf(CartNotFoundException.class)
                    .hasMessageContaining(cartId.toString());

            verify(checkoutOperationRepository).findByIdempotencyKey("idem-key");
            verifyNoMoreInteractions(checkoutOperationRepository, messagePublisher);
        }

        @Test
        @DisplayName("Should throw CartAlreadyPaidException when cart is already paid")
        void shouldThrowCartAlreadyPaidExceptionWhenCartIsAlreadyPaid() {
            // Given
            UUID cartId = UUID.randomUUID();
            ShoppingCart cart = createShoppingCart(cartId, "PAID");
            CreateAsyncCheckoutCommand command = createCommandWithCard(cartId, "CREDIT_CARD", "idem-key");

            when(shoppingCartRepository.findByPublicIdWithItems(cartId)).thenReturn(Optional.of(cart));

            // When / Then
            assertThatThrownBy(() -> asyncCheckoutService.initiateAsyncCheckout(command))
                    .isInstanceOf(CartAlreadyPaidException.class)
                    .hasMessageContaining(cartId.toString());
        }

        @Test
        @DisplayName("Should throw InvalidPaymentDetailsException when cart not in PENDING status")
        void shouldThrowInvalidPaymentDetailsExceptionWhenCartNotInPendingStatus() {
            // Given
            UUID cartId = UUID.randomUUID();
            ShoppingCart cart = createShoppingCart(cartId, "EXPIRED");
            CreateAsyncCheckoutCommand command = createCommandWithCard(cartId, "CREDIT_CARD", "idem-key");

            when(shoppingCartRepository.findByPublicIdWithItems(cartId)).thenReturn(Optional.of(cart));

            // When / Then
            assertThatThrownBy(() -> asyncCheckoutService.initiateAsyncCheckout(command))
                    .isInstanceOf(InvalidPaymentDetailsException.class)
                    .hasMessageContaining("Cart is not in PENDING status");
        }

        @Test
        @DisplayName("Should throw InvalidPaymentDetailsException when payment method not found")
        void shouldThrowInvalidPaymentDetailsExceptionWhenPaymentMethodNotFound() {
            // Given
            UUID cartId = UUID.randomUUID();
            ShoppingCart cart = createShoppingCart(cartId, "PENDING");
            CreateAsyncCheckoutCommand command = createCommandWithCard(cartId, "UNKNOWN_METHOD", "idem-key");

            when(shoppingCartRepository.findByPublicIdWithItems(cartId)).thenReturn(Optional.of(cart));
            when(paymentMethodsService.findByNormalizedName(anyString())).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> asyncCheckoutService.initiateAsyncCheckout(command))
                    .isInstanceOf(InvalidPaymentDetailsException.class)
                    .hasMessageContaining("Payment method type not offered");
        }

        @Test
        @DisplayName("Should throw InvalidPaymentDetailsException when payment method not active")
        void shouldThrowInvalidPaymentDetailsExceptionWhenPaymentMethodNotActive() {
            // Given
            UUID cartId = UUID.randomUUID();
            ShoppingCart cart = createShoppingCart(cartId, "PENDING");
            PaymentMethod paymentMethod = createPaymentMethod("Cartão de Crédito", "CREDIT_CARD", false);
            CreateAsyncCheckoutCommand command = createCommandWithCard(cartId, "CREDIT_CARD", "idem-key");

            when(shoppingCartRepository.findByPublicIdWithItems(cartId)).thenReturn(Optional.of(cart));
            when(paymentMethodsService.findByNormalizedName("CREDIT_CARD")).thenReturn(Optional.of(paymentMethod));

            // When / Then
            assertThatThrownBy(() -> asyncCheckoutService.initiateAsyncCheckout(command))
                    .isInstanceOf(InvalidPaymentDetailsException.class)
                    .hasMessageContaining("Payment method not available");
        }

        @Test
        @DisplayName("Should throw IdempotencyConflictException when idempotency key already exists")
        void shouldThrowIdempotencyConflictExceptionWhenIdempotencyKeyAlreadyExists() {
            // Given
            UUID cartId = UUID.randomUUID();
            String idempotencyKey = "existing-key";
            ShoppingCart cart = createShoppingCart(cartId, "PENDING");
            PaymentMethod paymentMethod = createPaymentMethod("Cartão de Crédito", "CREDIT_CARD", true);
            CheckoutOperation existingOperation = createCheckoutOperation(cart, paymentMethod);
            existingOperation.setIdempotencyKey(idempotencyKey);

            CreateAsyncCheckoutCommand command = createCommandWithCard(cartId, "CREDIT_CARD", idempotencyKey);

            when(checkoutOperationRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existingOperation));

            // When / Then
            assertThatThrownBy(() -> asyncCheckoutService.initiateAsyncCheckout(command))
                    .isInstanceOf(IdempotencyConflictException.class)
                    .hasMessageContaining(idempotencyKey);

            verify(checkoutOperationRepository).findByIdempotencyKey(idempotencyKey);
            verifyNoInteractions(shoppingCartRepository, paymentMethodsService, messagePublisher);
        }

        @Test
        @DisplayName("Should throw InvalidPaymentDetailsException when card CVV is missing")
        void shouldThrowInvalidPaymentDetailsExceptionWhenCardCvvMissing() {
            // Given
            UUID cartId = UUID.randomUUID();
            ShoppingCart cart = createShoppingCart(cartId, "PENDING");
            PaymentMethod paymentMethod = createPaymentMethod("Cartão de Crédito", "CREDIT_CARD", true);
            CreateAsyncCheckoutCommand command = createCommandWithCard(cartId, "CREDIT_CARD", "idem-key");
            command.getPaymentMethod().getCard().setCvv(null);

            when(shoppingCartRepository.findByPublicIdWithItems(cartId)).thenReturn(Optional.of(cart));
            when(paymentMethodsService.findByNormalizedName("CREDIT_CARD")).thenReturn(Optional.of(paymentMethod));

            // When / Then
            assertThatThrownBy(() -> asyncCheckoutService.initiateAsyncCheckout(command))
                    .isInstanceOf(InvalidPaymentDetailsException.class)
                    .hasMessageContaining("Card CVV is required");
        }

        @Test
        @DisplayName("Should throw InvalidPaymentDetailsException when PIX payer CPF is invalid")
        void shouldThrowInvalidPaymentDetailsExceptionWhenPixPayerCpfInvalid() {
            // Given
            UUID cartId = UUID.randomUUID();
            ShoppingCart cart = createShoppingCart(cartId, "PENDING");
            PaymentMethod paymentMethod = createPaymentMethod("PIX", "PIX", true);
            CreateAsyncCheckoutCommand command = createCommandWithPixPayer(cartId, "PIX", "idem-key", "John Doe", "11111111111"); // Invalid CPF

            when(shoppingCartRepository.findByPublicIdWithItems(cartId)).thenReturn(Optional.of(cart));
            when(paymentMethodsService.findByNormalizedName("PIX")).thenReturn(Optional.of(paymentMethod));

            // When / Then
            assertThatThrownBy(() -> asyncCheckoutService.initiateAsyncCheckout(command))
                    .isInstanceOf(InvalidPaymentDetailsException.class)
                    .hasMessageContaining("Invalid CPF");
        }

        @Test
        @DisplayName("Should throw InvalidPaymentDetailsException when PIX payer name is missing")
        void shouldThrowInvalidPaymentDetailsExceptionWhenPixPayerNameMissing() {
            // Given
            UUID cartId = UUID.randomUUID();
            ShoppingCart cart = createShoppingCart(cartId, "PENDING");
            PaymentMethod paymentMethod = createPaymentMethod("PIX", "PIX", true);
            CreateAsyncCheckoutCommand command = createCommandWithPixPayer(cartId, "PIX", "idem-key", "", "52998224725");

            when(shoppingCartRepository.findByPublicIdWithItems(cartId)).thenReturn(Optional.of(cart));
            when(paymentMethodsService.findByNormalizedName("PIX")).thenReturn(Optional.of(paymentMethod));

            // When / Then
            assertThatThrownBy(() -> asyncCheckoutService.initiateAsyncCheckout(command))
                    .isInstanceOf(InvalidPaymentDetailsException.class)
                    .hasMessageContaining("Payer name is required");
        }
    }

    @Nested
    @DisplayName("Get Checkout Status")
    class GetCheckoutStatus {

        @Test
        @DisplayName("Should return checkout status when operation exists")
        void shouldReturnCheckoutStatusWhenOperationExists() {
            // Given
            UUID checkoutId = UUID.randomUUID();
            CheckoutOperation operation = createCheckoutOperationWithId(checkoutId);
            AsyncCheckoutResponse expectedResponse = createAsyncCheckoutResponse(operation);

            when(checkoutOperationRepository.findByPublicId(checkoutId)).thenReturn(Optional.of(operation));
            when(asyncCheckoutMapper.toResponse(operation)).thenReturn(expectedResponse);

            // When
            AsyncCheckoutResponse response = asyncCheckoutService.getCheckoutStatus(checkoutId);

            // Then
            assertThat(response).isEqualTo(expectedResponse);
        }

        @Test
        @DisplayName("Should throw CheckoutOperationNotFoundException when operation not found")
        void shouldThrowCheckoutOperationNotFoundExceptionWhenOperationNotFound() {
            // Given
            UUID checkoutId = UUID.randomUUID();
            when(checkoutOperationRepository.findByPublicId(checkoutId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> asyncCheckoutService.getCheckoutStatus(checkoutId))
                    .isInstanceOf(CheckoutOperationNotFoundException.class)
                    .hasMessageContaining(checkoutId.toString());
        }
    }

    // Helper methods
    private ShoppingCart createShoppingCart(UUID publicId, String status) {
        ShoppingCart cart = new ShoppingCart();
        cart.setId(1L);
        cart.setPublicId(publicId);
        cart.setStatus(status);
        cart.setPrice(new com.checkout.bff.domain.valueobject.Money(BigDecimal.valueOf(100.00), Currency.getInstance("BRL")));
        return cart;
    }

    private PaymentMethod createPaymentMethod(String name, String normalizedName, boolean active) {
        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setId(1L);
        paymentMethod.setName(name);
        paymentMethod.setNormalizedName(normalizedName);
        paymentMethod.setActive(active);
        return paymentMethod;
    }

    private CheckoutOperation createCheckoutOperation(ShoppingCart cart, PaymentMethod paymentMethod) {
        CheckoutOperation operation = CheckoutOperation.builder()
                .cart(cart)
                .status("PENDING")
                .paymentMethodType("CREDIT_CARD")
                .paymentMethodName(paymentMethod.getName())
                .amount(BigDecimal.valueOf(100.00))
                .currency("BRL")
                .idempotencyKey("idem-key")
                .build();
        operation.setPublicId(UUID.randomUUID());
        return operation;
    }

    private CheckoutOperation createCheckoutOperationWithId(UUID checkoutId) {
        ShoppingCart cart = new ShoppingCart();
        cart.setPublicId(UUID.randomUUID());

        CheckoutOperation operation = CheckoutOperation.builder()
                .cart(cart)
                .status("PENDING")
                .paymentMethodType("CREDIT_CARD")
                .paymentMethodName("Cartão de Crédito")
                .amount(BigDecimal.valueOf(100.00))
                .currency("BRL")
                .build();
        operation.setPublicId(checkoutId);
        return operation;
    }

    private AsyncCheckoutResponse createAsyncCheckoutResponse(CheckoutOperation operation) {
        return AsyncCheckoutResponse.builder()
                .checkoutId(operation.getPublicId())
                .cartId(operation.getCart() != null ? operation.getCart().getPublicId() : UUID.randomUUID())
                .status(operation.getStatus())
                .createdAt(operation.getCreatedAt())
                .build();
    }

    private CreateAsyncCheckoutCommand createCommand(UUID cartId, String paymentType, String idempotencyKey) {
        return CreateAsyncCheckoutCommand.builder()
                .cartId(cartId)
                .paymentMethod(CreateAsyncCheckoutCommand.PaymentMethodData.builder()
                        .type(paymentType)
                        .build())
                .idempotencyKey(idempotencyKey)
                .build();
    }

    private CreateAsyncCheckoutCommand createCommandWithCard(UUID cartId, String paymentType, String idempotencyKey) {
        return CreateAsyncCheckoutCommand.builder()
                .cartId(cartId)
                .paymentMethod(CreateAsyncCheckoutCommand.PaymentMethodData.builder()
                        .type(paymentType)
                        .card(CreateAsyncCheckoutCommand.CardData.builder()
                                .holderName("John Doe")
                                .number("4111 1111 1111 1111")
                                .expiry("12/28")
                                .cvv("123")
                                .build())
                        .build())
                .idempotencyKey(idempotencyKey)
                .build();
    }

    private CreateAsyncCheckoutCommand createCommandWithPixPayer(UUID cartId, String paymentType, String idempotencyKey, String name, String cpf) {
        return CreateAsyncCheckoutCommand.builder()
                .cartId(cartId)
                .paymentMethod(CreateAsyncCheckoutCommand.PaymentMethodData.builder()
                        .type(paymentType)
                        .pixPayer(CreateAsyncCheckoutCommand.PixPayerData.builder()
                                .name(name)
                                .cpf(cpf)
                                .build())
                        .build())
                .idempotencyKey(idempotencyKey)
                .build();
    }
}