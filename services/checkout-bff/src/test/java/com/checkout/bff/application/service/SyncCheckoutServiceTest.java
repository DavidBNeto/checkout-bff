package com.checkout.bff.application.service;

import com.checkout.bff.application.dto.CreateSyncCheckoutCommand;
import com.checkout.bff.application.dto.SyncCheckoutResponse;
import com.checkout.bff.application.mapper.SyncCheckoutMapper;
import com.checkout.bff.application.port.CachePort;
import com.checkout.bff.application.port.PaymentMethodsServicePort;
import com.checkout.bff.domain.entity.CartItem;
import com.checkout.bff.domain.entity.CartPaymentMethod;
import com.checkout.bff.domain.entity.PaymentMethod;
import com.checkout.bff.domain.entity.Product;
import com.checkout.bff.domain.entity.ShoppingCart;
import com.checkout.bff.domain.exception.CartAlreadyPaidException;
import com.checkout.bff.domain.exception.CartNotFoundException;
import com.checkout.bff.domain.exception.EmptyCartException;
import com.checkout.bff.domain.exception.IdempotencyConflictException;
import com.checkout.bff.domain.exception.InvalidPaymentDetailsException;
import com.checkout.bff.domain.exception.PaymentDeclinedException;
import com.checkout.bff.domain.repository.ShoppingCartRepository;
import com.checkout.bff.domain.valueobject.Money;
import java.util.ArrayList;
import java.util.List;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncCheckoutServiceTest {

    @Mock
    private ShoppingCartRepository shoppingCartRepository;
    @Mock
    private PaymentMethodsServicePort paymentMethodsServicePort;
    @Mock
    private CachePort cachePort;
    @Mock
    private SyncCheckoutMapper syncCheckoutMapper;

    @Captor
    private ArgumentCaptor<ShoppingCart> cartCaptor;

    private SyncCheckoutService syncCheckoutService;

    @BeforeEach
    void setUp() {
        syncCheckoutService = new SyncCheckoutService(
                shoppingCartRepository,
                paymentMethodsServicePort,
                cachePort,
                syncCheckoutMapper
        );
    }

    @Nested
    @DisplayName("Execute Sync Checkout")
    class ExecuteSyncCheckout {

        @Test
        @DisplayName("Should process sync checkout successfully with CREDIT_CARD")
        void shouldProcessSyncCheckoutSuccessfullyWithCreditCard() {
            // Given
            UUID cartId = UUID.randomUUID();
            CreateSyncCheckoutCommand command = createCommand(cartId, "CREDIT_CARD", createCardData(), null); // Valid CPF
            ShoppingCart cart = createShoppingCart(cartId, "PENDING");
            setupAvailablePaymentMethods(cart, "Cartão de Crédito");

            when(shoppingCartRepository.findByPublicIdWithItems(cartId)).thenReturn(Optional.of(cart));
            when(shoppingCartRepository.save(any(ShoppingCart.class))).thenAnswer(inv -> inv.getArgument(0));
            when(syncCheckoutMapper.toResponse(any(ShoppingCart.class), any(CreateSyncCheckoutCommand.PaymentMethodData.class)))
                    .thenReturn(createSyncCheckoutResponse(cartId));

            // When
            SyncCheckoutResponse response = syncCheckoutService.execute(command, "idem-key-1");

            // Then
            assertThat(response).isNotNull();
            verify(shoppingCartRepository).save(cartCaptor.capture());
            ShoppingCart savedCart = cartCaptor.getValue();
            assertThat(savedCart.getStatus()).isEqualTo("PAID");
            assertThat(savedCart.getPaidAt()).isNotNull();
            verify(cachePort).delete("cart:" + cartId);
            verify(cachePort).set(eq("idempotency:idem-key-1"), anyString(), anyLong(), any());
        }

        @Test
        @DisplayName("Should process sync checkout successfully with PIX")
        void shouldProcessSyncCheckoutSuccessfullyWithPix() {
            // Given
            UUID cartId = UUID.randomUUID();
            CreateSyncCheckoutCommand command = createCommandWithPixPayer(cartId, "PIX", "John Doe", "52998224725"); // Valid CPF
            ShoppingCart cart = createShoppingCart(cartId, "PENDING");
            setupAvailablePaymentMethods(cart, "PIX");

            when(shoppingCartRepository.findByPublicIdWithItems(cartId)).thenReturn(Optional.of(cart));
            when(shoppingCartRepository.save(any(ShoppingCart.class))).thenAnswer(inv -> inv.getArgument(0));
            when(syncCheckoutMapper.toResponse(any(ShoppingCart.class), any(CreateSyncCheckoutCommand.PaymentMethodData.class)))
                    .thenReturn(createSyncCheckoutResponse(cartId));

            // When
            SyncCheckoutResponse response = syncCheckoutService.execute(command, "idem-key-2");

            // Then
            assertThat(response).isNotNull();
            verify(shoppingCartRepository).save(cartCaptor.capture());
            ShoppingCart savedCart = cartCaptor.getValue();
            assertThat(savedCart.getStatus()).isEqualTo("PAID");
        }

        @Test
        @DisplayName("Should throw CartNotFoundException when cart not found")
        void shouldThrowCartNotFoundExceptionWhenCartNotFound() {
            // Given
            UUID cartId = UUID.randomUUID();
            CreateSyncCheckoutCommand command = createCommand(cartId, "CREDIT_CARD", createCardData(), null);

            when(shoppingCartRepository.findByPublicIdWithItems(cartId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> syncCheckoutService.execute(command, "idem-key"))
                    .isInstanceOf(CartNotFoundException.class)
                    .hasMessageContaining(cartId.toString());
        }

        @Test
        @DisplayName("Should throw CartAlreadyPaidException when cart is already paid")
        void shouldThrowCartAlreadyPaidExceptionWhenCartIsAlreadyPaid() {
            // Given
            UUID cartId = UUID.randomUUID();
            CreateSyncCheckoutCommand command = createCommand(cartId, "CREDIT_CARD", createCardData(), null);
            ShoppingCart cart = createShoppingCart(cartId, "PAID");
            setupAvailablePaymentMethods(cart, "Cartão de Crédito");

            when(shoppingCartRepository.findByPublicIdWithItems(cartId)).thenReturn(Optional.of(cart));

            // When / Then
            assertThatThrownBy(() -> syncCheckoutService.execute(command, "idem-key"))
                    .isInstanceOf(CartAlreadyPaidException.class)
                    .hasMessageContaining(cartId.toString());
        }

        @Test
        @DisplayName("Should throw EmptyCartException when cart has no items")
        void shouldThrowEmptyCartExceptionWhenCartHasNoItems() {
            // Given
            UUID cartId = UUID.randomUUID();
            CreateSyncCheckoutCommand command = createCommand(cartId, "CREDIT_CARD", createCardData(), null);
            ShoppingCart cart = ShoppingCart.builder()
                    .publicId(cartId)
                    .price(Money.of(BigDecimal.valueOf(100.00), "BRL"))
                    .status("PENDING")
                    .build();
            setupAvailablePaymentMethods(cart, "Cartão de Crédito");

            when(shoppingCartRepository.findByPublicIdWithItems(cartId)).thenReturn(Optional.of(cart));

            // When / Then
            assertThatThrownBy(() -> syncCheckoutService.execute(command, "idem-key"))
                    .isInstanceOf(EmptyCartException.class);
        }

        @Test
        @DisplayName("Should throw InvalidPaymentDetailsException when payment method type not offered")
        void shouldThrowInvalidPaymentDetailsExceptionWhenPaymentMethodNotOffered() {
            // Given
            UUID cartId = UUID.randomUUID();
            CreateSyncCheckoutCommand command = createCommand(cartId, "PIX", null, null);
            ShoppingCart cart = createShoppingCart(cartId, "PENDING");
            setupAvailablePaymentMethods(cart, "Cartão de Crédito");

            when(shoppingCartRepository.findByPublicIdWithItems(cartId)).thenReturn(Optional.of(cart));

            // When / Then
            assertThatThrownBy(() -> syncCheckoutService.execute(command, "idem-key"))
                    .isInstanceOf(InvalidPaymentDetailsException.class)
                    .hasMessageContaining("Payment method not offered");
        }

        @Test
        @DisplayName("Should throw PaymentDeclinedException when card ends with 0002")
        void shouldThrowPaymentDeclinedExceptionWhenCardEndsWith0002() {
            // Given
            UUID cartId = UUID.randomUUID();
            CreateSyncCheckoutCommand.CardData card = createCardData();
            // 4000000000000002 is a valid Luhn number ending in 0002 (declined per business rule)
            card.setNumber("4000 0000 0000 0002");
            CreateSyncCheckoutCommand command = createCommand(cartId, "CREDIT_CARD", card, null);
            ShoppingCart cart = createShoppingCart(cartId, "PENDING");
            setupAvailablePaymentMethods(cart, "Cartão de Crédito");

            when(shoppingCartRepository.findByPublicIdWithItems(cartId)).thenReturn(Optional.of(cart));

            // When / Then
            assertThatThrownBy(() -> syncCheckoutService.execute(command, "idem-key"))
                    .isInstanceOf(PaymentDeclinedException.class);
        }

        @Test
        @DisplayName("Should approve payment when card ends with 0001")
        void shouldApprovePaymentWhenCardEndsWith0001() {
            // Given
            UUID cartId = UUID.randomUUID();
            CreateSyncCheckoutCommand.CardData card = createCardData();
            // 9000000000000001 is a valid Luhn number ending in 0001 (approved per business rule)
            card.setNumber("9000 0000 0000 0001");
            CreateSyncCheckoutCommand command = createCommand(cartId, "CREDIT_CARD", card, null);
            ShoppingCart cart = createShoppingCart(cartId, "PENDING");
            setupAvailablePaymentMethods(cart, "Cartão de Crédito");

            when(shoppingCartRepository.findByPublicIdWithItems(cartId)).thenReturn(Optional.of(cart));
            when(shoppingCartRepository.save(any(ShoppingCart.class))).thenAnswer(inv -> inv.getArgument(0));
            when(syncCheckoutMapper.toResponse(any(ShoppingCart.class), any(CreateSyncCheckoutCommand.PaymentMethodData.class)))
                    .thenReturn(createSyncCheckoutResponse(cartId));

            // When
            SyncCheckoutResponse response = syncCheckoutService.execute(command, "idem-key");

            // Then
            assertThat(response).isNotNull();
            verify(shoppingCartRepository).save(cartCaptor.capture());
            assertThat(cartCaptor.getValue().getStatus()).isEqualTo("PAID");
        }

        @Test
        @DisplayName("Should throw IdempotencyConflictException when idempotency key already used")
        void shouldThrowIdempotencyConflictExceptionWhenIdempotencyKeyAlreadyUsed() {
            // Given
            UUID cartId = UUID.randomUUID();
            CreateSyncCheckoutCommand command = createCommand(cartId, "CREDIT_CARD", createCardData(), null);
            ShoppingCart cart = createShoppingCart(cartId, "PENDING");
            setupAvailablePaymentMethods(cart, "Cartão de Crédito");

            when(cachePort.get(eq("idempotency:idem-key"), eq(String.class))).thenReturn(Optional.of(cartId.toString()));

            // When / Then
            assertThatThrownBy(() -> syncCheckoutService.execute(command, "idem-key"))
                    .isInstanceOf(IdempotencyConflictException.class)
                    .hasMessageContaining("idem-key");
        }

        @Test
        @DisplayName("Should throw InvalidPaymentDetailsException when card CVV is missing")
        void shouldThrowInvalidPaymentDetailsExceptionWhenCardCvvMissing() {
            // Given
            UUID cartId = UUID.randomUUID();
            CreateSyncCheckoutCommand command = createCommand(cartId, "CREDIT_CARD", createCardData(), null);
            command.getPaymentMethod().getCard().setCvv(null);
            ShoppingCart cart = createShoppingCart(cartId, "PENDING");
            setupAvailablePaymentMethods(cart, "Cartão de Crédito");

            when(shoppingCartRepository.findByPublicIdWithItems(cartId)).thenReturn(Optional.of(cart));

            // When / Then
            assertThatThrownBy(() -> syncCheckoutService.execute(command, "idem-key"))
                    .isInstanceOf(InvalidPaymentDetailsException.class)
                    .hasMessageContaining("Card CVV is required");
        }

        @Test
        @DisplayName("Should throw InvalidPaymentDetailsException for invalid PIX payer CPF")
        void shouldThrowInvalidPaymentDetailsExceptionForInvalidPixPayerCpf() {
            // Given
            UUID cartId = UUID.randomUUID();
            CreateSyncCheckoutCommand command = createCommandWithPixPayer(cartId, "PIX", "John Doe", "11111111111"); // Invalid CPF
            ShoppingCart cart = createShoppingCart(cartId, "PENDING");
            setupAvailablePaymentMethods(cart, "PIX");

            when(shoppingCartRepository.findByPublicIdWithItems(cartId)).thenReturn(Optional.of(cart));

            // When / Then
            assertThatThrownBy(() -> syncCheckoutService.execute(command, "idem-key"))
                    .isInstanceOf(InvalidPaymentDetailsException.class)
                    .hasMessageContaining("Invalid CPF");
        }

        @Test
        @DisplayName("Should throw InvalidPaymentDetailsException when PIX payer name is missing")
        void shouldThrowInvalidPaymentDetailsExceptionWhenPixPayerNameMissing() {
            // Given
            UUID cartId = UUID.randomUUID();
            CreateSyncCheckoutCommand command = createCommandWithPixPayer(cartId, "PIX", "", "52998224725");
            ShoppingCart cart = createShoppingCart(cartId, "PENDING");
            setupAvailablePaymentMethods(cart, "PIX");

            when(shoppingCartRepository.findByPublicIdWithItems(cartId)).thenReturn(Optional.of(cart));

            // When / Then
            assertThatThrownBy(() -> syncCheckoutService.execute(command, "idem-key"))
                    .isInstanceOf(InvalidPaymentDetailsException.class)
                    .hasMessageContaining("Payer name is required");
        }

        @Test
        @DisplayName("Should approve PIX payment with valid CPF")
        void shouldApprovePixPaymentWithValidCpf() {
            // Given
            UUID cartId = UUID.randomUUID();
            CreateSyncCheckoutCommand command = createCommandWithPixPayer(cartId, "PIX", "John Doe", "52998224725"); // Valid CPF
            ShoppingCart cart = createShoppingCart(cartId, "PENDING");
            setupAvailablePaymentMethods(cart, "PIX");

            when(cachePort.get(anyString(), any())).thenReturn(Optional.empty());
            when(shoppingCartRepository.findByPublicIdWithItems(cartId)).thenReturn(Optional.of(cart));
            when(shoppingCartRepository.save(any(ShoppingCart.class))).thenAnswer(inv -> inv.getArgument(0));
            when(syncCheckoutMapper.toResponse(any(ShoppingCart.class), any(CreateSyncCheckoutCommand.PaymentMethodData.class)))
                    .thenReturn(createSyncCheckoutResponse(cartId));

            // When
            SyncCheckoutResponse response = syncCheckoutService.execute(command, "idem-key");

            // Then
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Should not check idempotency when key is null")
        void shouldNotCheckIdempotencyWhenKeyIsNull() {
            // Given
            UUID cartId = UUID.randomUUID();
            CreateSyncCheckoutCommand command = createCommand(cartId, "CREDIT_CARD", createCardData(), null);
            ShoppingCart cart = createShoppingCart(cartId, "PENDING");
            setupAvailablePaymentMethods(cart, "Cartão de Crédito");

            when(shoppingCartRepository.findByPublicIdWithItems(cartId)).thenReturn(Optional.of(cart));
            when(shoppingCartRepository.save(any(ShoppingCart.class))).thenAnswer(inv -> inv.getArgument(0));
            when(syncCheckoutMapper.toResponse(any(ShoppingCart.class), any(CreateSyncCheckoutCommand.PaymentMethodData.class)))
                    .thenReturn(createSyncCheckoutResponse(cartId));

            // When
            SyncCheckoutResponse response = syncCheckoutService.execute(command, null);

            // Then
            assertThat(response).isNotNull();
            verify(cachePort, never()).get(anyString(), any());
        }
    }

    // Helper methods
    private CreateSyncCheckoutCommand createCommand(UUID cartId, String type, CreateSyncCheckoutCommand.CardData card, String pixKey) {
        CreateSyncCheckoutCommand.PaymentMethodData.PaymentMethodDataBuilder paymentMethodBuilder = CreateSyncCheckoutCommand.PaymentMethodData.builder()
                .type(type);
        if (card != null) {
            paymentMethodBuilder.card(card);
        }
        return CreateSyncCheckoutCommand.builder()
                .cartId(cartId)
                .paymentMethod(paymentMethodBuilder.build())
                .build();
    }

    private CreateSyncCheckoutCommand createCommandWithPixPayer(UUID cartId, String type, String name, String cpf) {
        CreateSyncCheckoutCommand.PixPayerData pixPayer = CreateSyncCheckoutCommand.PixPayerData.builder()
                .name(name)
                .cpf(cpf)
                .build();
        CreateSyncCheckoutCommand.PaymentMethodData paymentMethod = CreateSyncCheckoutCommand.PaymentMethodData.builder()
                .type(type)
                .pixPayer(pixPayer)
                .build();
        return CreateSyncCheckoutCommand.builder()
                .cartId(cartId)
                .paymentMethod(paymentMethod)
                .build();
    }

    private CreateSyncCheckoutCommand.CardData createCardData() {
        // 4111 1111 1111 1111 is a valid Luhn number (Visa test card)
        return CreateSyncCheckoutCommand.CardData.builder()
                .holderName("John Doe")
                .number("4111 1111 1111 1111")
                .expiry("12/28")
                .cvv("123")
                .build();
    }

    private ShoppingCart createShoppingCart(UUID publicId, String status) {
        ShoppingCart cart = ShoppingCart.builder()
                .publicId(publicId)
                .price(Money.of(BigDecimal.valueOf(100.00), "BRL"))
                .status(status)
                .build();
        cart.addItem(CartItem.builder()
                .product(createProduct())
                .quantity(1)
                .build());
        return cart;
    }

    private Product createProduct() {
        Product product = new Product();
        product.setId(1L);
        product.setSlug("product-1");
        product.setName("Product 1");
        product.setPrice(Money.of(BigDecimal.valueOf(100.00), "BRL"));
        product.setActive(true);
        return product;
    }

    private void setupAvailablePaymentMethods(ShoppingCart cart, String... methodNames) {
        List<CartPaymentMethod> methods = new ArrayList<>();
        for (int i = 0; i < methodNames.length; i++) {
            String methodName = methodNames[i];
            PaymentMethod paymentMethod = new PaymentMethod();
            paymentMethod.setId((long) (i + 1));
            paymentMethod.setName(methodName);
            paymentMethod.setNormalizedName(mapPaymentMethodNameToType(methodName));
            paymentMethod.setActive(true);

            CartPaymentMethod cartPaymentMethod = CartPaymentMethod.builder()
                    .cartId(cart.getId())
                    .paymentMethod(paymentMethod)
                    .paymentMethodName(methodName)
                    .build();
            methods.add(cartPaymentMethod);
        }
        cart.setAvailablePaymentMethods(methods);
    }

    private String mapPaymentMethodNameToType(String name) {
        return switch (name.toLowerCase()) {
            case "credit-card", "cartao de credito", "cartão de crédito" -> "CREDIT_CARD";
            case "debit-card", "cartao de debito", "cartão de débito" -> "DEBIT_CARD";
            case "pix" -> "PIX";
            default -> name.toUpperCase();
        };
    }

    private SyncCheckoutResponse createSyncCheckoutResponse(UUID cartId) {
        return SyncCheckoutResponse.builder()
                .cartId(cartId)
                .status("PAID")
                .build();
    }
}