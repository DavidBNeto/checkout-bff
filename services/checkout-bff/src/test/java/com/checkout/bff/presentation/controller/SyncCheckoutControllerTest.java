package com.checkout.bff.presentation.controller;

import com.checkout.bff.application.dto.CreateSyncCheckoutCommand;
import com.checkout.bff.application.dto.SyncCheckoutResponse;
import com.checkout.bff.application.usecase.SyncCheckoutUseCase;
import com.checkout.bff.presentation.dto.SyncCheckoutRequest;
import com.checkout.bff.presentation.mapper.SyncCheckoutPresentationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncCheckoutControllerTest {

    @Mock
    private SyncCheckoutUseCase syncCheckoutUseCase;

    @Mock
    private SyncCheckoutPresentationMapper presentationMapper;

    @Captor
    private ArgumentCaptor<CreateSyncCheckoutCommand> commandCaptor;

    private SyncCheckoutController syncCheckoutController;

    @BeforeEach
    void setUp() {
        syncCheckoutController = new SyncCheckoutController(syncCheckoutUseCase, presentationMapper);
    }

    @Nested
    @DisplayName("Sync Checkout")
    class SyncCheckout {

        @Test
        @DisplayName("Should process sync checkout with credit card successfully")
        void shouldProcessSyncCheckoutWithCreditCardSuccessfully() {
            // Given
            SyncCheckoutRequest request = createSyncCheckoutRequestWithCreditCard();
            SyncCheckoutResponse applicationResponse = createApplicationResponse();
            com.checkout.bff.presentation.dto.SyncCheckoutResponse presentationResponse = createPresentationResponse();

            lenient().when(syncCheckoutUseCase.execute(any(CreateSyncCheckoutCommand.class), anyString())).thenReturn(applicationResponse);
            lenient().when(presentationMapper.toResponse(any(SyncCheckoutResponse.class))).thenReturn(presentationResponse);

            // When
            ResponseEntity<com.checkout.bff.presentation.dto.SyncCheckoutResponse> response = syncCheckoutController.syncCheckout(request, "idem-key-123");

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isEqualTo(presentationResponse);
            verify(syncCheckoutUseCase).execute(commandCaptor.capture(), eq("idem-key-123"));

            CreateSyncCheckoutCommand capturedCommand = commandCaptor.getValue();
            assertThat(capturedCommand.getCartId()).isEqualTo(request.getCartId());
            assertThat(capturedCommand.getPaymentMethod().getType()).isEqualTo("CREDIT_CARD");
            assertThat(capturedCommand.getPaymentMethod().getCard()).isNotNull();
        }

        @Test
        @DisplayName("Should process sync checkout with PIX successfully")
        void shouldProcessSyncCheckoutWithPixSuccessfully() {
            // Given
            SyncCheckoutRequest request = createSyncCheckoutRequestWithPix();
            SyncCheckoutResponse applicationResponse = createApplicationResponse();
            com.checkout.bff.presentation.dto.SyncCheckoutResponse presentationResponse = createPresentationResponse();

            lenient().when(syncCheckoutUseCase.execute(any(CreateSyncCheckoutCommand.class), anyString())).thenReturn(applicationResponse);
            lenient().when(presentationMapper.toResponse(any(SyncCheckoutResponse.class))).thenReturn(presentationResponse);

            // When
            ResponseEntity<com.checkout.bff.presentation.dto.SyncCheckoutResponse> response = syncCheckoutController.syncCheckout(request, "idem-key-123");

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isEqualTo(presentationResponse);
            verify(syncCheckoutUseCase).execute(commandCaptor.capture(), eq("idem-key-123"));

            CreateSyncCheckoutCommand capturedCommand = commandCaptor.getValue();
            assertThat(capturedCommand.getPaymentMethod().getType()).isEqualTo("PIX");
            assertThat(capturedCommand.getPaymentMethod().getPixPayer()).isNotNull();
            assertThat(capturedCommand.getPaymentMethod().getPixPayer().getCpf()).isEqualTo("52998224725");
            assertThat(capturedCommand.getPaymentMethod().getPixPayer().getName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("Should process sync checkout without idempotency key")
        void shouldProcessSyncCheckoutWithoutIdempotencyKey() {
            // Given
            SyncCheckoutRequest request = createSyncCheckoutRequestWithCreditCard();
            SyncCheckoutResponse applicationResponse = createApplicationResponse();
            com.checkout.bff.presentation.dto.SyncCheckoutResponse presentationResponse = createPresentationResponse();

            lenient().when(syncCheckoutUseCase.execute(any(CreateSyncCheckoutCommand.class), isNull())).thenReturn(applicationResponse);
            lenient().when(presentationMapper.toResponse(any(SyncCheckoutResponse.class))).thenReturn(presentationResponse);

            // When
            ResponseEntity<com.checkout.bff.presentation.dto.SyncCheckoutResponse> response = syncCheckoutController.syncCheckout(request, null);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            verify(syncCheckoutUseCase).execute(commandCaptor.capture(), isNull());
        }

        @Test
        @DisplayName("Should process sync checkout with empty idempotency key")
        void shouldProcessSyncCheckoutWithEmptyIdempotencyKey() {
            // Given
            SyncCheckoutRequest request = createSyncCheckoutRequestWithCreditCard();
            SyncCheckoutResponse applicationResponse = createApplicationResponse();
            com.checkout.bff.presentation.dto.SyncCheckoutResponse presentationResponse = createPresentationResponse();

            lenient().when(syncCheckoutUseCase.execute(any(CreateSyncCheckoutCommand.class), anyString())).thenReturn(applicationResponse);
            lenient().when(presentationMapper.toResponse(any(SyncCheckoutResponse.class))).thenReturn(presentationResponse);

            // When
            ResponseEntity<com.checkout.bff.presentation.dto.SyncCheckoutResponse> response = syncCheckoutController.syncCheckout(request, "   ");

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            verify(syncCheckoutUseCase).execute(commandCaptor.capture(), eq("   "));
        }
    }

    private SyncCheckoutRequest createSyncCheckoutRequestWithCreditCard() {
        return SyncCheckoutRequest.builder()
                .cartId(UUID.randomUUID())
                .paymentMethod(SyncCheckoutRequest.PaymentMethodData.builder()
                        .type("CREDIT_CARD")
                        .card(SyncCheckoutRequest.CardData.builder()
                                .holderName("John Doe")
                                .number("4111111111111111")
                                .expiry("12/28")
                                .cvv("123")
                                .build())
                        .build())
                .build();
    }

    private SyncCheckoutRequest createSyncCheckoutRequestWithPix() {
        return SyncCheckoutRequest.builder()
                .cartId(UUID.randomUUID())
                .paymentMethod(SyncCheckoutRequest.PaymentMethodData.builder()
                        .type("PIX")
                        .pixPayer(SyncCheckoutRequest.PixPayerData.builder()
                                .name("John Doe")
                                .cpf("52998224725")
                                .build())
                        .build())
                .build();
    }

    private SyncCheckoutResponse createApplicationResponse() {
        return SyncCheckoutResponse.builder()
                .cartId(UUID.randomUUID())
                .status("APPROVED")
                .paidAt(LocalDateTime.now())
                .payment(SyncCheckoutResponse.PaymentDetails.builder()
                        .method("Cartão de Crédito")
                        .lastDigits("1111")
                        .amount(BigDecimal.valueOf(100.00))
                        .currency("BRL")
                        .build())
                .products(List.of())
                .build();
    }

    private com.checkout.bff.presentation.dto.SyncCheckoutResponse createPresentationResponse() {
        return com.checkout.bff.presentation.dto.SyncCheckoutResponse.builder()
                .cartId(UUID.randomUUID())
                .status("APPROVED")
                .paidAt(LocalDateTime.now())
                .payment(com.checkout.bff.presentation.dto.SyncCheckoutResponse.PaymentDetails.builder()
                        .method("Cartão de Crédito")
                        .lastDigits("1111")
                        .amount(BigDecimal.valueOf(100.00))
                        .currency("BRL")
                        .build())
                .products(List.of())
                .build();
    }
}