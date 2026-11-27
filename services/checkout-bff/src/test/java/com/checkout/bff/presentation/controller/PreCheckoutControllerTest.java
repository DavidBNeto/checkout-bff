package com.checkout.bff.presentation.controller;

import com.checkout.bff.application.dto.CreatePreCheckoutCommand;
import com.checkout.bff.application.dto.PreCheckoutResponse;
import com.checkout.bff.application.usecase.CreatePreCheckoutUseCase;
import com.checkout.bff.presentation.dto.PreCheckoutPresentationResponse;
import com.checkout.bff.presentation.dto.PreCheckoutRequest;
import com.checkout.bff.presentation.mapper.PreCheckoutPresentationMapper;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreCheckoutControllerTest {

    @Mock
    private CreatePreCheckoutUseCase createPreCheckoutUseCase;

    @Mock
    private PreCheckoutPresentationMapper presentationMapper;

    @Captor
    private ArgumentCaptor<CreatePreCheckoutCommand> commandCaptor;

    private PreCheckoutController preCheckoutController;

    @BeforeEach
    void setUp() {
        preCheckoutController = new PreCheckoutController(createPreCheckoutUseCase, presentationMapper);
    }

    @Nested
    @DisplayName("Create Pre-Checkout")
    class CreatePreCheckout {

        @Test
        @DisplayName("Should create pre-checkout successfully")
        void shouldCreatePreCheckoutSuccessfully() {
            // Given
            PreCheckoutRequest request = createPreCheckoutRequest("product-1", "product-2");
            PreCheckoutResponse applicationResponse = createApplicationResponse();
            PreCheckoutPresentationResponse presentationResponse = createPresentationResponse();

            lenient().when(createPreCheckoutUseCase.execute(any(CreatePreCheckoutCommand.class))).thenReturn(applicationResponse);
            lenient().when(presentationMapper.toResponse(any(PreCheckoutResponse.class))).thenReturn(presentationResponse);

            // When
            ResponseEntity<PreCheckoutPresentationResponse> response = preCheckoutController.createPreCheckout(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isEqualTo(presentationResponse);
            verify(createPreCheckoutUseCase).execute(commandCaptor.capture());

            CreatePreCheckoutCommand capturedCommand = commandCaptor.getValue();
            assertThat(capturedCommand.getProducts()).hasSize(2);
            assertThat(capturedCommand.getProducts().get(0).getSlug()).isEqualTo("product-1");
            assertThat(capturedCommand.getProducts().get(1).getSlug()).isEqualTo("product-2");
        }

        @Test
        @DisplayName("Should map request to command correctly with single product")
        void shouldMapRequestToCommandCorrectlyWithSingleProduct() {
            // Given
            PreCheckoutRequest request = createPreCheckoutRequest("product-1");
            PreCheckoutResponse applicationResponse = createApplicationResponse();
            PreCheckoutPresentationResponse presentationResponse = createPresentationResponse();

            lenient().when(createPreCheckoutUseCase.execute(any(CreatePreCheckoutCommand.class))).thenReturn(applicationResponse);
            lenient().when(presentationMapper.toResponse(any(PreCheckoutResponse.class))).thenReturn(presentationResponse);

            // When
            preCheckoutController.createPreCheckout(request);

            // Then
            verify(createPreCheckoutUseCase).execute(commandCaptor.capture());
            CreatePreCheckoutCommand capturedCommand = commandCaptor.getValue();
            assertThat(capturedCommand.getProducts()).hasSize(1);
            assertThat(capturedCommand.getProducts().get(0).getSlug()).isEqualTo("product-1");
        }

        @Test
        @DisplayName("Should map request to command correctly with empty product list")
        void shouldMapRequestToCommandCorrectlyWithEmptyProductList() {
            // Given
            PreCheckoutRequest request = PreCheckoutRequest.builder()
                    .products(List.of())
                    .build();
            PreCheckoutResponse applicationResponse = createApplicationResponse();
            PreCheckoutPresentationResponse presentationResponse = createPresentationResponse();

            lenient().when(createPreCheckoutUseCase.execute(any(CreatePreCheckoutCommand.class))).thenReturn(applicationResponse);
            lenient().when(presentationMapper.toResponse(any(PreCheckoutResponse.class))).thenReturn(presentationResponse);

            // When
            preCheckoutController.createPreCheckout(request);

            // Then
            verify(createPreCheckoutUseCase).execute(commandCaptor.capture());
            CreatePreCheckoutCommand capturedCommand = commandCaptor.getValue();
            assertThat(capturedCommand.getProducts()).isEmpty();
        }
    }

    private PreCheckoutRequest createPreCheckoutRequest(String... slugs) {
        List<PreCheckoutRequest.ProductSelection> products = List.of();
        if (slugs != null) {
            products = java.util.Arrays.stream(slugs)
                    .map(slug -> PreCheckoutRequest.ProductSelection.builder().slug(slug).build())
                    .toList();
        }
        return PreCheckoutRequest.builder()
                .products(products)
                .build();
    }

    private CreatePreCheckoutCommand createCommand(String... slugs) {
        List<CreatePreCheckoutCommand.ProductSelection> products = List.of();
        if (slugs != null) {
            products = java.util.Arrays.stream(slugs)
                    .map(slug -> CreatePreCheckoutCommand.ProductSelection.builder().slug(slug).build())
                    .toList();
        }
        return CreatePreCheckoutCommand.builder()
                .products(products)
                .build();
    }

    private PreCheckoutResponse createApplicationResponse() {
        return PreCheckoutResponse.builder()
                .cartId(UUID.randomUUID())
                .products(List.of())
                .paymentMethods(List.of())
                .build();
    }

    private PreCheckoutPresentationResponse createPresentationResponse() {
        return PreCheckoutPresentationResponse.builder()
                .cartId(UUID.randomUUID())
                .products(List.of())
                .paymentMethods(List.of())
                .build();
    }
}