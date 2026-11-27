package com.checkout.bff.presentation.controller;

import com.checkout.bff.application.dto.CreateAsyncCheckoutCommand;
import com.checkout.bff.application.usecase.AsyncCheckoutUseCase;
import com.checkout.bff.presentation.dto.AsyncCheckoutRequest;
import com.checkout.bff.presentation.dto.AsyncCheckoutResponse;
import com.checkout.bff.presentation.mapper.AsyncCheckoutPresentationMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/operations")
@RequiredArgsConstructor
public class AsyncCheckoutController {

    private final AsyncCheckoutUseCase asyncCheckoutUseCase;
    private final AsyncCheckoutPresentationMapper presentationMapper;

    @PostMapping("/checkout/async")
    public ResponseEntity<AsyncCheckoutResponse> initiateAsyncCheckout(
            @Valid @RequestBody AsyncCheckoutRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        log.info("Received async checkout request for cart: {}", request.getCartId());

        // Use header idempotency key if provided, otherwise use request body
        String effectiveIdempotencyKey = idempotencyKey != null ? idempotencyKey : request.getIdempotencyKey();

        var command = CreateAsyncCheckoutCommand.builder()
                .cartId(request.getCartId())
                .paymentMethod(mapPaymentMethod(request.getPaymentMethod()))
                .idempotencyKey(effectiveIdempotencyKey)
                .build();

        var applicationResponse = asyncCheckoutUseCase.initiateAsyncCheckout(command);
        var response = presentationMapper.toResponse(applicationResponse);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    private CreateAsyncCheckoutCommand.PaymentMethodData mapPaymentMethod(
            AsyncCheckoutRequest.PaymentMethodData source) {
        if (source == null) {
            return null;
        }
        return CreateAsyncCheckoutCommand.PaymentMethodData.builder()
                .type(source.getType())
                .card(mapCardData(source.getCard()))
                .pixPayer(mapPixPayerData(source.getPixPayer()))
                .build();
    }

    private CreateAsyncCheckoutCommand.PixPayerData mapPixPayerData(
            AsyncCheckoutRequest.PixPayerData source) {
        if (source == null) {
            return null;
        }
        return CreateAsyncCheckoutCommand.PixPayerData.builder()
                .name(source.getName())
                .cpf(source.getCpf())
                .build();
    }

    private CreateAsyncCheckoutCommand.CardData mapCardData(
            AsyncCheckoutRequest.CardData source) {
        if (source == null) {
            return null;
        }
        return CreateAsyncCheckoutCommand.CardData.builder()
                .holderName(source.getHolderName())
                .number(source.getNumber())
                .expiry(source.getExpiry())
                .cvv(source.getCvv())
                .build();
    }

    @GetMapping("/checkouts/{checkoutId}")
    public ResponseEntity<AsyncCheckoutResponse> getCheckoutStatus(
            @PathVariable UUID checkoutId) {

        log.info("Getting async checkout status for: {}", checkoutId);

        var applicationResponse = asyncCheckoutUseCase.getCheckoutStatus(checkoutId);
        var response = presentationMapper.toResponse(applicationResponse);

        return ResponseEntity.ok(response);
    }
}