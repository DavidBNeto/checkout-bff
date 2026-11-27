package com.checkout.bff.presentation.controller;

import com.checkout.bff.application.dto.CreateSyncCheckoutCommand;
import com.checkout.bff.application.usecase.SyncCheckoutUseCase;
import com.checkout.bff.presentation.dto.SyncCheckoutResponse;
import com.checkout.bff.presentation.dto.SyncCheckoutRequest;
import com.checkout.bff.presentation.mapper.SyncCheckoutPresentationMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/operations")
@RequiredArgsConstructor
public class SyncCheckoutController {

    private final SyncCheckoutUseCase syncCheckoutUseCase;
    private final SyncCheckoutPresentationMapper presentationMapper;

    @PostMapping("/checkout")
    public ResponseEntity<SyncCheckoutResponse> syncCheckout(
            @Valid @RequestBody SyncCheckoutRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        log.info("Received sync checkout request for cart: {}", request.getCartId());

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            log.warn("Sync checkout request without Idempotency-Key header for cart: {}", request.getCartId());
        }

        // Map presentation request to application command
        CreateSyncCheckoutCommand command = mapToCommand(request);

        // Execute use case
        var applicationResponse = syncCheckoutUseCase.execute(command, idempotencyKey);

        // Map application response to presentation response
        SyncCheckoutResponse response = presentationMapper.toResponse(applicationResponse);

        log.info("Sync checkout completed successfully: cartId={}, status={}", response.getCartId(), response.getStatus());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private CreateSyncCheckoutCommand mapToCommand(SyncCheckoutRequest request) {
        CreateSyncCheckoutCommand.PaymentMethodData paymentMethod = CreateSyncCheckoutCommand.PaymentMethodData.builder()
                .type(request.getPaymentMethod().getType())
                .build();

        if (request.getPaymentMethod().getCard() != null) {
            CreateSyncCheckoutCommand.CardData card = CreateSyncCheckoutCommand.CardData.builder()
                    .holderName(request.getPaymentMethod().getCard().getHolderName())
                    .number(request.getPaymentMethod().getCard().getNumber())
                    .expiry(request.getPaymentMethod().getCard().getExpiry())
                    .cvv(request.getPaymentMethod().getCard().getCvv())
                    .build();
            paymentMethod.setCard(card);
        }

        if (request.getPaymentMethod().getPixPayer() != null) {
            CreateSyncCheckoutCommand.PixPayerData pixPayer = CreateSyncCheckoutCommand.PixPayerData.builder()
                    .name(request.getPaymentMethod().getPixPayer().getName())
                    .cpf(request.getPaymentMethod().getPixPayer().getCpf())
                    .build();
            paymentMethod.setPixPayer(pixPayer);
        }

        return CreateSyncCheckoutCommand.builder()
                .cartId(request.getCartId())
                .paymentMethod(paymentMethod)
                .build();
    }
}