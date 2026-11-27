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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound(ProductNotFoundException ex, HttpServletRequest request) {
        log.warn("Product not found: {}", ex.getSlug());
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "PRODUCT_NOT_FOUND",
                "P-1004",
                "Produto não encontrado",
                "Os seguintes produtos não foram encontrados: " + ex.getSlug(),
                "Retornar",
                "/",
                null
        );
    }

    @ExceptionHandler(ProductUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleProductUnavailable(ProductUnavailableException ex, HttpServletRequest request) {
        log.warn("Product unavailable: {}", ex.getSlug());
        return buildErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "PRODUCT_UNAVAILABLE",
                "P-1005",
                "Produto indisponível",
                "Os seguintes não estão disponíveis: " + ex.getSlug(),
                "Retornar",
                "/",
                null
        );
    }

    @ExceptionHandler(EmptyCartException.class)
    public ResponseEntity<ErrorResponse> handleEmptyCart(EmptyCartException ex, HttpServletRequest request) {
        log.warn("Empty cart request");
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "P-1007",
                "Carrinho Inválido",
                "Pelo menos um produto é necessário.",
                "Tentar novamente",
                "RETRY",
                null
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation error: {}", ex.getMessage());
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "P-1007",
                "Carrinho Inválido",
                "Erro de validação: " + errors,
                "Tentar novamente",
                "RETRY",
                null
        );
    }

    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCartNotFound(CartNotFoundException ex, HttpServletRequest request) {
        log.warn("Cart not found: {}", ex.getCartId());
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "INVALID_CART",
                "P-1000",
                "Carrinho inválido",
                "Carrinho não encontrado.",
                "Tentar novamente",
                "RETRY",
                ex.getCartId()
        );
    }

    @ExceptionHandler(CartAlreadyPaidException.class)
    public ResponseEntity<ErrorResponse> handleCartAlreadyPaid(CartAlreadyPaidException ex, HttpServletRequest request) {
        log.warn("Cart already paid: {}", ex.getCartId());
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "ALREADY_PAID",
                "P-1002",
                "Carrinho já pago",
                "Não foi possível concluir o pagamento: carrinho já foi previamente pago.",
                "Voltar",
                "/",
                ex.getCartId()
        );
    }

    @ExceptionHandler(InvalidPaymentDetailsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPaymentDetails(InvalidPaymentDetailsException ex, HttpServletRequest request) {
        log.warn("Invalid payment details for cart {}: {}", ex.getCartId(), ex.getReason());
        return buildErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "INVALID_PAYMENT_DETAILS",
                "P-1001",
                "Método de pagamento inválido",
                "Erro validando o método de pagamento, tente novamente com outro método.",
                "Tentar novamente",
                "RETRY",
                ex.getCartId()
        );
    }

    @ExceptionHandler(PaymentDeclinedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentDeclined(PaymentDeclinedException ex, HttpServletRequest request) {
        log.warn("Payment declined for cart {}: {}", ex.getCartId(), ex.getPaymentMethod());
        ErrorResponse.FrontendError frontendError = ErrorResponse.FrontendError.builder()
                .code("P-1001")
                .title("Pagamento não aprovado")
                .description("Não foi possível concluir o pagamento. Tente novamente ou escolha outro meio.")
                .primaryButton(ErrorResponse.FrontendError.Button.builder()
                        .label("Tentar novamente")
                        .url("RETRY")
                        .build())
                .secondaryButton(ErrorResponse.FrontendError.Button.builder()
                        .label("Trocar meio de pagamento")
                        .url("/checkout/pagamento")
                        .build())
                .build();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .displayType("BLOCK")
                .errorCode("PAYMENT_DECLINED")
                .cartId(ex.getCartId())
                .frontendError(frontendError)
                .build();

        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(errorResponse);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyConflict(IdempotencyConflictException ex, HttpServletRequest request) {
        log.warn("Idempotency conflict for key: {}", ex.getIdempotencyKey());
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "IDEMPOTENCY_CONFLICT",
                "P-1006",
                "Requisição duplicada",
                "Uma requisição com a mesma chave de idempotência já está sendo processada: " + ex.getIdempotencyKey(),
                "Tentar novamente",
                "RETRY",
                null
        );
    }

    @ExceptionHandler(CheckoutOperationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCheckoutOperationNotFound(CheckoutOperationNotFoundException ex, HttpServletRequest request) {
        log.warn("Checkout operation not found: {}", ex.getCheckoutId());
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "CHECKOUT_NOT_FOUND",
                "P-1008",
                "Operação de checkout não encontrada",
                "A operação de checkout solicitada não foi encontrada.",
                "Tentar novamente",
                "RETRY",
                null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "P-9998",
                "Erro interno inesperado",
                "Um erro inesperado aconteceu, tente novamente mais tarde.",
                "Tentar novamente",
                "RETRY",
                null
        );
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status,
                                                              String errorCode,
                                                              String frontendCode,
                                                              String title,
                                                              String description,
                                                              String primaryLabel,
                                                              String primaryUrl,
                                                              String cartId) {
        ErrorResponse.FrontendError frontendError = ErrorResponse.FrontendError.builder()
                .code(frontendCode)
                .title(title)
                .description(description)
                .primaryButton(ErrorResponse.FrontendError.Button.builder()
                        .label(primaryLabel)
                        .url(primaryUrl)
                        .build())
                .build();

        ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder()
                .displayType("BLOCK")
                .errorCode(errorCode)
                .frontendError(frontendError);

        if (cartId != null) {
            builder.cartId(cartId);
        }

        return ResponseEntity.status(status).body(builder.build());
    }
}