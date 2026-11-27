package com.checkout.payment.application.service;

import com.checkout.payment.application.port.MessagePublisherPort;
import com.checkout.payment.domain.entity.PaymentMethod;
import com.checkout.payment.domain.repository.PaymentMethodRepository;
import com.checkout.payment.infrastructure.messaging.event.PaymentProcessedEvent;
import com.checkout.payment.infrastructure.redis.ProcessedEventStore;
import com.checkout.payment.infrastructure.messaging.event.OrderCreatedEvent;
import br.com.caelum.stella.validation.CPFValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessPaymentService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final MessagePublisherPort messagePublisher;
    private final ProcessedEventStore processedEventStore;

    public boolean isAlreadyProcessed(String checkoutId) {
        return processedEventStore.isProcessed(checkoutId);
    }

    @Transactional
    public void processPayment(OrderCreatedEvent event) {
        String checkoutId = event.getCheckoutId();
        log.info("Processing payment for checkout: {}", checkoutId);

        // Check idempotency
        if (isAlreadyProcessed(checkoutId)) {
            log.warn("Payment already processed for checkout: {}", checkoutId);
            return;
        }

        try {
            // Validate payment method exists and is active
            Long paymentMethodId = event.getPaymentMethodId();
            Optional<PaymentMethod> paymentMethodOpt = paymentMethodRepository.findById(paymentMethodId);
            if (paymentMethodOpt.isEmpty() || !paymentMethodOpt.get().isActive()) {
                log.warn("Payment method not found or inactive: {}", paymentMethodId);
                throw new IllegalStateException("Payment method not available: " + paymentMethodId);
            }

            // Simulate payment processing
            String status;
            String failureReason = null;
            String authorizationCode = null;
            String maskedCardNumber = null;
            String pixCode = null;

            String paymentMethodType = event.getPaymentDetails().getType();
            if ("PIX".equals(paymentMethodType)) {
                // Validate PIX payer CPF
                var pixPayer = event.getPixPayer();
                if (pixPayer == null || pixPayer.getCpf() == null || pixPayer.getCpf().isBlank()) {
                    status = "FAILED";
                    failureReason = "PIX payment requires payer name and CPF";
                } else if (!isValidCpf(pixPayer.getCpf())) {
                    status = "FAILED";
                    failureReason = "Invalid CPF: " + pixPayer.getCpf();
                } else {
                    status = "APPROVED";
                    pixCode = generatePixCode(checkoutId);
                }
            } else {
                status = "FAILED";
                failureReason = "Unknown payment method type: " + paymentMethodType;
            }

            // Mark as processed for idempotency
            processedEventStore.markProcessed(checkoutId, checkoutId);

            // Publish PaymentProcessedEvent
            PaymentProcessedEvent processedEvent = buildPaymentProcessedEvent(event, status, failureReason,
                    authorizationCode, maskedCardNumber, pixCode);
            messagePublisher.publishPaymentProcessed(processedEvent);
            log.info("Published PaymentProcessedEvent for checkout: {}", checkoutId);

        } catch (Exception e) {
            log.error("Payment processing failed for checkout: {}", checkoutId, e);

            // Publish failure event
            PaymentProcessedEvent failedEvent = buildPaymentProcessedEvent(event, "FAILED", e.getMessage(),
                    null, null, null);
            messagePublisher.publishPaymentProcessed(failedEvent);
        }
    }

    private String generatePixCode(String checkoutId) {
        return "PIX|" + UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    /**
     * Validates a Brazilian CPF using Stella library.
     * Removes formatting characters and validates check digits.
     */
    private boolean isValidCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return false;
        }
        // Remove non-digit characters (dots, dashes)
        String cleanCpf = cpf.replaceAll("[^0-9]", "");
        if (cleanCpf.length() != 11) {
            return false;
        }
        CPFValidator validator = new CPFValidator();
        try {
            validator.assertValid(cleanCpf);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private PaymentProcessedEvent buildPaymentProcessedEvent(
            com.checkout.payment.infrastructure.messaging.event.OrderCreatedEvent event,
            String status,
            String failureReason,
            String authorizationCode,
            String maskedCardNumber,
            String pixCode) {

        PaymentProcessedEvent.PaymentResultEvent result = PaymentProcessedEvent.PaymentResultEvent.builder()
                .authorizationCode(authorizationCode)
                .maskedCardNumber(maskedCardNumber)
                .pixCode(pixCode)
                .failureReason(failureReason)
                .build();

        return PaymentProcessedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PAYMENT_PROCESSED")
                .checkoutId(event.getCheckoutId())
                .cartId(event.getCartId())
                .status(status)
                .paymentMethodName(event.getPaymentMethodName())
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .result(result)
                .timestamp(java.time.Instant.now())
                .correlationId(event.getCheckoutId())
                .build();
    }
}