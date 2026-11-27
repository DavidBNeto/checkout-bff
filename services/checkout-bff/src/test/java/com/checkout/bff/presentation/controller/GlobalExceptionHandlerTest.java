package com.checkout.bff.presentation.controller;

import com.checkout.bff.domain.exception.CartAlreadyPaidException;
import com.checkout.bff.domain.exception.CartNotFoundException;
import com.checkout.bff.domain.exception.CheckoutOperationNotFoundException;
import com.checkout.bff.domain.exception.EmptyCartException;
import com.checkout.bff.domain.exception.IdempotencyConflictException;
import com.checkout.bff.domain.exception.InvalidPaymentDetailsException;
import com.checkout.bff.domain.exception.PaymentDeclinedException;
import com.checkout.bff.domain.exception.ProductNotFoundException;
import com.checkout.bff.domain.exception.ProductUnavailableException;
import com.checkout.bff.presentation.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("ProductNotFoundException")
    class ProductNotFoundExceptionTest {

        @Test
        @DisplayName("Should return BAD_REQUEST with product not found error")
        void shouldReturnBadRequestWithProductNotFoundError() {
            // Given
            ProductNotFoundException ex = new ProductNotFoundException("product-123");

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleProductNotFound(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("PRODUCT_NOT_FOUND");
            assertThat(response.getBody().getFrontendError().getCode()).isEqualTo("P-1004");
            assertThat(response.getBody().getFrontendError().getDescription()).contains("product-123");
        }
    }

    @Nested
    @DisplayName("ProductUnavailableException")
    class ProductUnavailableExceptionTest {

        @Test
        @DisplayName("Should return UNPROCESSABLE_ENTITY with product unavailable error")
        void shouldReturnUnprocessableEntityWithProductUnavailableError() {
            // Given
            ProductUnavailableException ex = new ProductUnavailableException("product-123");

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleProductUnavailable(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("PRODUCT_UNAVAILABLE");
            assertThat(response.getBody().getFrontendError().getCode()).isEqualTo("P-1005");
            assertThat(response.getBody().getFrontendError().getDescription()).contains("product-123");
        }
    }

    @Nested
    @DisplayName("EmptyCartException")
    class EmptyCartExceptionTest {

        @Test
        @DisplayName("Should return BAD_REQUEST with empty cart error")
        void shouldReturnBadRequestWithEmptyCartError() {
            // Given
            EmptyCartException ex = new EmptyCartException();

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleEmptyCart(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("INVALID_REQUEST");
            assertThat(response.getBody().getFrontendError().getCode()).isEqualTo("P-1007");
            assertThat(response.getBody().getFrontendError().getDescription()).contains("Pelo menos um produto");
        }
    }

    @Nested
    @DisplayName("MethodArgumentNotValidException")
    class MethodArgumentNotValidExceptionTest {

        @Test
        @DisplayName("Should return BAD_REQUEST with validation errors")
        void shouldReturnBadRequestWithValidationErrors() throws NoSuchMethodException {
            // Given
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError fieldError = new FieldError("request", "cartId", "must not be null");
            when(bindingResult.getAllErrors()).thenReturn(Collections.singletonList(fieldError));

            Method method = MethodArgumentNotValidExceptionTest.class.getDeclaredMethod("dummyMethod");
            MethodParameter methodParameter = new MethodParameter(method, -1);
            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidation(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("INVALID_REQUEST");
            assertThat(response.getBody().getFrontendError().getCode()).isEqualTo("P-1007");
            assertThat(response.getBody().getFrontendError().getDescription()).contains("cartId");
        }

        private void dummyMethod() {}
    }

    @Nested
    @DisplayName("CartNotFoundException")
    class CartNotFoundExceptionTest {

        @Test
        @DisplayName("Should return NOT_FOUND with cart not found error")
        void shouldReturnNotFoundWithCartNotFoundError() {
            // Given
            String cartId = UUID.randomUUID().toString();
            CartNotFoundException ex = new CartNotFoundException(cartId);

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleCartNotFound(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("INVALID_CART");
            assertThat(response.getBody().getFrontendError().getCode()).isEqualTo("P-1000");
            assertThat(response.getBody().getCartId()).isEqualTo(cartId);
        }
    }

    @Nested
    @DisplayName("CartAlreadyPaidException")
    class CartAlreadyPaidExceptionTest {

        @Test
        @DisplayName("Should return CONFLICT with cart already paid error")
        void shouldReturnConflictWithCartAlreadyPaidError() {
            // Given
            String cartId = UUID.randomUUID().toString();
            CartAlreadyPaidException ex = new CartAlreadyPaidException(cartId);

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleCartAlreadyPaid(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("ALREADY_PAID");
            assertThat(response.getBody().getFrontendError().getCode()).isEqualTo("P-1002");
            assertThat(response.getBody().getCartId()).isEqualTo(cartId);
        }
    }

    @Nested
    @DisplayName("InvalidPaymentDetailsException")
    class InvalidPaymentDetailsExceptionTest {

        @Test
        @DisplayName("Should return UNPROCESSABLE_ENTITY with invalid payment details error")
        void shouldReturnUnprocessableEntityWithInvalidPaymentDetailsError() {
            // Given
            String cartId = UUID.randomUUID().toString();
            InvalidPaymentDetailsException ex = new InvalidPaymentDetailsException(cartId, "Invalid card number");

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidPaymentDetails(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("INVALID_PAYMENT_DETAILS");
            assertThat(response.getBody().getFrontendError().getCode()).isEqualTo("P-1001");
            assertThat(response.getBody().getCartId()).isEqualTo(cartId);
        }
    }

    @Nested
    @DisplayName("PaymentDeclinedException")
    class PaymentDeclinedExceptionTest {

        @Test
        @DisplayName("Should return PAYMENT_REQUIRED with payment declined error")
        void shouldReturnPaymentRequiredWithPaymentDeclinedError() {
            // Given
            String cartId = UUID.randomUUID().toString();
            PaymentDeclinedException ex = new PaymentDeclinedException(cartId, "CREDIT_CARD", "Payment declined by issuer");

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handlePaymentDeclined(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("PAYMENT_DECLINED");
            assertThat(response.getBody().getFrontendError().getCode()).isEqualTo("P-1001");
            assertThat(response.getBody().getFrontendError().getTitle()).isEqualTo("Pagamento não aprovado");
            assertThat(response.getBody().getFrontendError().getPrimaryButton().getLabel()).isEqualTo("Tentar novamente");
            assertThat(response.getBody().getFrontendError().getSecondaryButton().getLabel()).isEqualTo("Trocar meio de pagamento");
            assertThat(response.getBody().getCartId()).isEqualTo(cartId);
        }
    }

    @Nested
    @DisplayName("IdempotencyConflictException")
    class IdempotencyConflictExceptionTest {

        @Test
        @DisplayName("Should return CONFLICT with idempotency conflict error")
        void shouldReturnConflictWithIdempotencyConflictError() {
            // Given
            IdempotencyConflictException ex = new IdempotencyConflictException("idem-key-123");

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleIdempotencyConflict(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("IDEMPOTENCY_CONFLICT");
            assertThat(response.getBody().getFrontendError().getCode()).isEqualTo("P-1006");
            assertThat(response.getBody().getFrontendError().getDescription()).contains("idem-key-123");
        }
    }

    @Nested
    @DisplayName("CheckoutOperationNotFoundException")
    class CheckoutOperationNotFoundExceptionTest {

        @Test
        @DisplayName("Should return NOT_FOUND with checkout operation not found error")
        void shouldReturnNotFoundWithCheckoutOperationNotFoundError() {
            // Given
            CheckoutOperationNotFoundException ex = new CheckoutOperationNotFoundException(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleCheckoutOperationNotFound(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("CHECKOUT_NOT_FOUND");
            assertThat(response.getBody().getFrontendError().getCode()).isEqualTo("P-1008");
        }
    }

    @Nested
    @DisplayName("Generic Exception")
    class GenericExceptionTest {

        @Test
        @DisplayName("Should return INTERNAL_SERVER_ERROR for unexpected errors")
        void shouldReturnInternalServerErrorForUnexpectedErrors() {
            // Given
            RuntimeException ex = new RuntimeException("Unexpected error");

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleGeneric(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("INTERNAL_ERROR");
            assertThat(response.getBody().getFrontendError().getCode()).isEqualTo("P-9998");
        }
    }
}