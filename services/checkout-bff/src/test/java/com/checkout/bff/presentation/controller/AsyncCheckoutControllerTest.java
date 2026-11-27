package com.checkout.bff.presentation.controller;

import com.checkout.bff.application.dto.CreateAsyncCheckoutCommand;
import com.checkout.bff.application.dto.AsyncCheckoutResponse;
import com.checkout.bff.application.usecase.AsyncCheckoutUseCase;
import com.checkout.bff.presentation.dto.AsyncCheckoutRequest;
import com.checkout.bff.presentation.mapper.AsyncCheckoutPresentationMapper;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncCheckoutControllerTest {

    @Mock
    private AsyncCheckoutUseCase asyncCheckoutUseCase;

    @Mock
    private AsyncCheckoutPresentationMapper presentationMapper;

    @Captor
    private ArgumentCaptor<CreateAsyncCheckoutCommand> commandCaptor;

    private AsyncCheckoutController asyncCheckoutController;

    @BeforeEach
    void setUp() {
        asyncCheckoutController = new AsyncCheckoutController(asyncCheckoutUseCase, presentationMapper);
    }

    @Nested
    @DisplayName("Initiate Async Checkout")
    class InitiateAsyncCheckout {

        @Test
        @DisplayName("Should initiate async checkout with credit card successfully")
        void shouldInitiateAsyncCheckoutWithCreditCardSuccessfully() {
            // Given
            AsyncCheckoutRequest request = createAsyncCheckoutRequestWithCreditCard();
            AsyncCheckoutResponse applicationResponse = createApplicationResponse();
            com.checkout.bff.presentation.dto.AsyncCheckoutResponse presentationResponse = createPresentationResponse();

            lenient().when(asyncCheckoutUseCase.initiateAsyncCheckout(any(CreateAsyncCheckoutCommand.class))).thenReturn(applicationResponse);
            lenient().when(presentationMapper.toResponse(any(AsyncCheckoutResponse.class))).thenReturn(presentationResponse);

            // When
            ResponseEntity<com.checkout.bff.presentation.dto.AsyncCheckoutResponse> response = asyncCheckoutController.initiateAsyncCheckout(request, "idem-key-123");

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            assertThat(response.getBody()).isEqualTo(presentationResponse);
            verify(asyncCheckoutUseCase).initiateAsyncCheckout(commandCaptor.capture());

            CreateAsyncCheckoutCommand capturedCommand = commandCaptor.getValue();
            assertThat(capturedCommand.getCartId()).isEqualTo(request.getCartId());
            assertThat(capturedCommand.getPaymentMethod().getType()).isEqualTo("CREDIT_CARD");
            assertThat(capturedCommand.getPaymentMethod().getCard()).isNotNull();
            assertThat(capturedCommand.getIdempotencyKey()).isEqualTo("idem-key-123");
        }

        @Test
        @DisplayName("Should initiate async checkout with PIX successfully")
        void shouldInitiateAsyncCheckoutWithPixSuccessfully() {
            // Given
            AsyncCheckoutRequest request = createAsyncCheckoutRequestWithPix();
            AsyncCheckoutResponse applicationResponse = createApplicationResponse();
            com.checkout.bff.presentation.dto.AsyncCheckoutResponse presentationResponse = createPresentationResponse();

            lenient().when(asyncCheckoutUseCase.initiateAsyncCheckout(any(CreateAsyncCheckoutCommand.class))).thenReturn(applicationResponse);
            lenient().when(presentationMapper.toResponse(any(AsyncCheckoutResponse.class))).thenReturn(presentationResponse);

            // When
            ResponseEntity<com.checkout.bff.presentation.dto.AsyncCheckoutResponse> response = asyncCheckoutController.initiateAsyncCheckout(request, "idem-key-123");

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            assertThat(response.getBody()).isEqualTo(presentationResponse);
            verify(asyncCheckoutUseCase).initiateAsyncCheckout(commandCaptor.capture());

            CreateAsyncCheckoutCommand capturedCommand = commandCaptor.getValue();
            assertThat(capturedCommand.getPaymentMethod().getType()).isEqualTo("PIX");
            assertThat(capturedCommand.getPaymentMethod().getPixPayer()).isNotNull();
            assertThat(capturedCommand.getPaymentMethod().getPixPayer().getName()).isEqualTo("John Doe");
            assertThat(capturedCommand.getPaymentMethod().getPixPayer().getCpf()).isEqualTo("52998224725");
        }

        @Test
        @DisplayName("Should use request body idempotency key when header not provided")
        void shouldUseRequestBodyIdempotencyKeyWhenHeaderNotProvided() {
            // Given
            AsyncCheckoutRequest request = createAsyncCheckoutRequestWithCreditCard();
            request.setIdempotencyKey("body-idem-key");
            AsyncCheckoutResponse applicationResponse = createApplicationResponse();
            com.checkout.bff.presentation.dto.AsyncCheckoutResponse presentationResponse = createPresentationResponse();

            lenient().when(asyncCheckoutUseCase.initiateAsyncCheckout(any(CreateAsyncCheckoutCommand.class))).thenReturn(applicationResponse);
            lenient().when(presentationMapper.toResponse(any(AsyncCheckoutResponse.class))).thenReturn(presentationResponse);

            // When
            ResponseEntity<com.checkout.bff.presentation.dto.AsyncCheckoutResponse> response = asyncCheckoutController.initiateAsyncCheckout(request, null);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            verify(asyncCheckoutUseCase).initiateAsyncCheckout(commandCaptor.capture());

            CreateAsyncCheckoutCommand capturedCommand = commandCaptor.getValue();
            assertThat(capturedCommand.getIdempotencyKey()).isEqualTo("body-idem-key");
        }

        @Test
        @DisplayName("Should prioritize header idempotency key over request body")
        void shouldPrioritizeHeaderIdempotencyKeyOverRequestBody() {
            // Given
            AsyncCheckoutRequest request = createAsyncCheckoutRequestWithCreditCard();
            request.setIdempotencyKey("body-idem-key");
            AsyncCheckoutResponse applicationResponse = createApplicationResponse();
            com.checkout.bff.presentation.dto.AsyncCheckoutResponse presentationResponse = createPresentationResponse();

            lenient().when(asyncCheckoutUseCase.initiateAsyncCheckout(any(CreateAsyncCheckoutCommand.class))).thenReturn(applicationResponse);
            lenient().when(presentationMapper.toResponse(any(AsyncCheckoutResponse.class))).thenReturn(presentationResponse);

            // When
            ResponseEntity<com.checkout.bff.presentation.dto.AsyncCheckoutResponse> response = asyncCheckoutController.initiateAsyncCheckout(request, "header-idem-key");

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            verify(asyncCheckoutUseCase).initiateAsyncCheckout(commandCaptor.capture());

            CreateAsyncCheckoutCommand capturedCommand = commandCaptor.getValue();
            assertThat(capturedCommand.getIdempotencyKey()).isEqualTo("header-idem-key");
        }
    }

    @Nested
    @DisplayName("Get Checkout Status")
    class GetCheckoutStatus {

        @Test
        @DisplayName("Should return checkout status successfully")
        void shouldReturnCheckoutStatusSuccessfully() {
            // Given
            UUID checkoutId = UUID.randomUUID();
            AsyncCheckoutResponse applicationResponse = createApplicationResponse();
            com.checkout.bff.presentation.dto.AsyncCheckoutResponse presentationResponse = createPresentationResponse();

            lenient().when(asyncCheckoutUseCase.getCheckoutStatus(checkoutId)).thenReturn(applicationResponse);
            lenient().when(presentationMapper.toResponse(any(AsyncCheckoutResponse.class))).thenReturn(presentationResponse);

            // When
            ResponseEntity<com.checkout.bff.presentation.dto.AsyncCheckoutResponse> response = asyncCheckoutController.getCheckoutStatus(checkoutId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(presentationResponse);
            verify(asyncCheckoutUseCase).getCheckoutStatus(checkoutId);
        }
    }

    private AsyncCheckoutRequest createAsyncCheckoutRequestWithCreditCard() {
        return AsyncCheckoutRequest.builder()
                .cartId(UUID.randomUUID())
                .paymentMethod(AsyncCheckoutRequest.PaymentMethodData.builder()
                        .type("CREDIT_CARD")
                        .card(AsyncCheckoutRequest.CardData.builder()
                                .holderName("John Doe")
                                .number("4111111111111111")
                                .expiry("12/28")
                                .cvv("123")
                                .build())
                        .build())
                .build();
    }

    private AsyncCheckoutRequest createAsyncCheckoutRequestWithPix() {
        return AsyncCheckoutRequest.builder()
                .cartId(UUID.randomUUID())
                .paymentMethod(AsyncCheckoutRequest.PaymentMethodData.builder()
                        .type("PIX")
                        .pixPayer(AsyncCheckoutRequest.PixPayerData.builder()
                                .name("John Doe")
                                .cpf("52998224725")
                                .build())
                        .build())
                .build();
    }

    private AsyncCheckoutResponse createApplicationResponse() {
        return AsyncCheckoutResponse.builder()
                .checkoutId(UUID.randomUUID())
                .cartId(UUID.randomUUID())
                .status("PENDING")
                .build();
    }

    private com.checkout.bff.presentation.dto.AsyncCheckoutResponse createPresentationResponse() {
        return com.checkout.bff.presentation.dto.AsyncCheckoutResponse.builder()
                .checkoutId(UUID.randomUUID())
                .cartId(UUID.randomUUID())
                .status("PENDING")
                .build();
    }
}