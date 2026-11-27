package com.checkout.bff.application.service;

import com.checkout.bff.application.usecase.ProcessPaymentUseCase;
import com.checkout.bff.domain.entity.CheckoutOperation;
import com.checkout.bff.domain.entity.ShoppingCart;
import com.checkout.bff.domain.repository.CheckoutOperationRepository;
import com.checkout.bff.domain.repository.ShoppingCartRepository;
import com.checkout.bff.infrastructure.messaging.event.PaymentProcessedEvent;
import com.checkout.bff.infrastructure.redis.ProcessedEventStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessPaymentService implements ProcessPaymentUseCase {

    private final CheckoutOperationRepository checkoutOperationRepository;
    private final ShoppingCartRepository shoppingCartRepository;
    private final ProcessedEventStore processedEventStore;

    @Override
    public boolean isAlreadyProcessed(String checkoutId) {
        return processedEventStore.isProcessed(checkoutId);
    }

    @Override
    @Transactional
    public void execute(PaymentProcessedEvent event) {
        String checkoutId = event.getCheckoutId();
        log.info("Received PaymentProcessedEvent for checkout: {} with status: {}", checkoutId, event.getStatus());

        // Check idempotency
        if (isAlreadyProcessed(checkoutId)) {
            log.warn("Payment already processed for checkout: {}", checkoutId);
            return;
        }

        // Find checkout operation
        var operationOptional = checkoutOperationRepository.findByPublicId(UUID.fromString(checkoutId));
        if (operationOptional.isEmpty()) {
            log.warn("Checkout operation not found: {}", checkoutId);
            return;
        }

        CheckoutOperation operation = operationOptional.get();

        // Check if already in terminal state
        if (operation.isTerminal()) {
            log.warn("Checkout operation already in terminal state: {} - {}", checkoutId, operation.getStatus());
            return;
        }

        // Update operation with result from payment service
        String status = event.getStatus();
        operation.setStatus(status);
        operation.setProcessedAt(LocalDateTime.now());
        operation.setAuthorizationCode(event.getResult() != null ? event.getResult().getAuthorizationCode() : null);
        operation.setMaskedCardNumber(event.getResult() != null ? event.getResult().getMaskedCardNumber() : null);
        operation.setPixCode(event.getResult() != null ? event.getResult().getPixCode() : null);
        operation.setFailureReason(event.getResult() != null ? event.getResult().getFailureReason() : null);
        checkoutOperationRepository.save(operation);

        // If approved, update cart to PAID
        if ("APPROVED".equals(status)) {
            var cartOptional = shoppingCartRepository.findById(operation.getCart().getId());
            if (cartOptional.isPresent()) {
                ShoppingCart cart = cartOptional.get();
                cart.setStatus("PAID");
                cart.setPaidAt(LocalDateTime.now());
                shoppingCartRepository.save(cart);
            }
        }

        // Mark as processed for idempotency
        processedEventStore.markProcessed(checkoutId, checkoutId);

        log.info("Updated checkout operation {} with status {}", checkoutId, status);
    }
}