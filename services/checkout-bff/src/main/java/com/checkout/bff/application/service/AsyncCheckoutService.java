package com.checkout.bff.application.service;

import com.checkout.bff.application.dto.AsyncCheckoutResponse;
import com.checkout.bff.application.dto.CreateAsyncCheckoutCommand;
import com.checkout.bff.application.mapper.AsyncCheckoutMapper;
import com.checkout.bff.application.port.PaymentMethodsServicePort;
import com.checkout.bff.application.usecase.AsyncCheckoutUseCase;
import com.checkout.bff.domain.entity.CheckoutOperation;
import com.checkout.bff.domain.entity.PaymentMethod;
import com.checkout.bff.domain.entity.ShoppingCart;
import com.checkout.bff.domain.exception.CartAlreadyPaidException;
import com.checkout.bff.domain.exception.CartNotFoundException;
import com.checkout.bff.domain.exception.CheckoutOperationNotFoundException;
import com.checkout.bff.domain.exception.IdempotencyConflictException;
import com.checkout.bff.domain.exception.InvalidPaymentDetailsException;
import com.checkout.bff.domain.repository.CheckoutOperationRepository;
import com.checkout.bff.domain.repository.ShoppingCartRepository;
import com.checkout.bff.infrastructure.messaging.MessagePublisherPort;
import com.checkout.bff.infrastructure.messaging.event.OrderCreatedEvent;
import com.checkout.bff.infrastructure.messaging.event.PaymentDetailsEvent;
import com.checkout.bff.infrastructure.messaging.event.PixPayerEvent;
import br.com.caelum.stella.validation.CPFValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncCheckoutService implements AsyncCheckoutUseCase {

    private final ShoppingCartRepository shoppingCartRepository;
    private final PaymentMethodsServicePort paymentMethodsService;
    private final CheckoutOperationRepository checkoutOperationRepository;
    private final AsyncCheckoutMapper asyncCheckoutMapper;
    private final MessagePublisherPort messagePublisher;

    @Override
    @Transactional
    public AsyncCheckoutResponse initiateAsyncCheckout(CreateAsyncCheckoutCommand command) {
        log.info("Initiating async checkout for cart: {}", command.getCartId());

        // Check idempotency
        if (command.getIdempotencyKey() != null) {
            var existing = checkoutOperationRepository.findByIdempotencyKey(command.getIdempotencyKey());
            if (existing.isPresent()) {
                log.warn("Idempotency conflict for key: {}", command.getIdempotencyKey());
                throw new IdempotencyConflictException(command.getIdempotencyKey());
            }
        }

        // Find and validate cart
        var cartOptional = shoppingCartRepository.findByPublicIdWithItems(command.getCartId());
        if (cartOptional.isEmpty()) {
            log.warn("Cart not found: {}", command.getCartId());
            throw new CartNotFoundException(command.getCartId().toString());
        }
        ShoppingCart cart = cartOptional.get();

        if (!cart.isPending()) {
            if (cart.isPaid()) {
                log.warn("Cart already paid: {}", command.getCartId());
                throw new CartAlreadyPaidException(command.getCartId().toString());
            }
            log.warn("Cart not in PENDING status: {} - status: {}", command.getCartId(), cart.getStatus());
            throw new InvalidPaymentDetailsException(command.getCartId().toString(), "Cart is not in PENDING status");
        }

        // Validate payment method type was offered for this cart
        String normalizedType = command.getPaymentMethod().getType().toUpperCase();
        var paymentMethodOptional = paymentMethodsService.findByNormalizedName(normalizedType);
        if (paymentMethodOptional.isEmpty()) {
            log.warn("Payment method not found for type: {}", normalizedType);
            throw new InvalidPaymentDetailsException(command.getCartId().toString(),
                    "Payment method type not offered for this cart: " + normalizedType);
        }
        PaymentMethod paymentMethod = paymentMethodOptional.get();

        if (!paymentMethod.isActive()) {
            log.warn("Payment method not active: {}", paymentMethod.getName());
            throw new InvalidPaymentDetailsException(command.getCartId().toString(),
                    "Payment method not available: " + paymentMethod.getName());
        }

        // Validate payment details based on payment method type
        validatePaymentDetails(command.getPaymentMethod(), normalizedType, cart.getPublicId().toString());

        // Create checkout operation
        CheckoutOperation operation = CheckoutOperation.builder()
                .cart(cart)
                .status("PENDING")
                .paymentMethodType(normalizedType)
                .paymentMethodName(paymentMethod.getName())
                .amount(cart.getPrice().getAmount())
                .currency(cart.getPrice().getCurrency().getCurrencyCode())
                .idempotencyKey(command.getIdempotencyKey())
                .build();

        operation = checkoutOperationRepository.save(operation);
        log.info("Created checkout operation: {}", operation.getPublicId());

        // Publish OrderCreatedEvent to outbox for async processing
        OrderCreatedEvent event = buildOrderCreatedEvent(operation, cart, paymentMethod, command);
        messagePublisher.publishOrderCreated(event);
        log.info("Published OrderCreatedEvent for checkout: {}", operation.getPublicId());

        return asyncCheckoutMapper.toResponse(operation);
    }

    @Override
    @Transactional(readOnly = true)
    public AsyncCheckoutResponse getCheckoutStatus(UUID checkoutId) {
        log.debug("Getting checkout status for: {}", checkoutId);

        var operationOptional = checkoutOperationRepository.findByPublicId(checkoutId);
        if (operationOptional.isEmpty()) {
            log.warn("Checkout operation not found: {}", checkoutId);
            throw new CheckoutOperationNotFoundException(checkoutId);
        }

        return asyncCheckoutMapper.toResponse(operationOptional.get());
    }

    private OrderCreatedEvent buildOrderCreatedEvent(CheckoutOperation operation,
                                                      ShoppingCart cart,
                                                      PaymentMethod paymentMethod,
                                                      CreateAsyncCheckoutCommand command) {
        String maskedData = buildMaskedData(operation.getPaymentMethodType(), operation.getPublicId());

        PaymentDetailsEvent paymentDetails = new PaymentDetailsEvent(
                operation.getPaymentMethodType(),
                maskedData
        );

        // Build PIX payer event if PIX payment method
        PixPayerEvent pixPayer = null;
        if ("PIX".equalsIgnoreCase(operation.getPaymentMethodType()) && command.getPaymentMethod() != null) {
            var pixPayerData = command.getPaymentMethod().getPixPayer();
            if (pixPayerData != null) {
                pixPayer = new PixPayerEvent(pixPayerData.getName(), pixPayerData.getCpf());
            }
        }

        // Build card payer event if card payment method (no CPF required for cards)
        String cardPayerCpf = null;

        return new OrderCreatedEvent(
                operation.getPublicId().toString(),
                "ORDER_CREATED",
                cart.getPublicId().toString(),
                operation.getPublicId().toString(),
                paymentMethod.getId(),
                paymentMethod.getName(),
                operation.getAmount(),
                operation.getCurrency(),
                paymentDetails,
                pixPayer,
                cardPayerCpf,
                Instant.now(),
                operation.getPublicId().toString()
        );
    }

    private String buildMaskedData(String paymentMethodType, UUID checkoutId) {
        // For async checkout, we don't have the actual card details yet
        // Return a placeholder that will be filled in by the payment worker
        return switch (paymentMethodType) {
            case "CREDIT_CARD", "DEBIT_CARD" -> "****"; // Will be updated with last 4 digits
            case "PIX" -> "pix-" + checkoutId.toString().substring(0, 8);
            default -> "****";
        };
    }

    private void validatePaymentDetails(CreateAsyncCheckoutCommand.PaymentMethodData paymentMethod,
                                         String paymentMethodType,
                                         String cartId) {
        if ("CREDIT_CARD".equalsIgnoreCase(paymentMethodType)
                || "DEBIT_CARD".equalsIgnoreCase(paymentMethodType)) {
            if (paymentMethod.getCard() == null) {
                throw new InvalidPaymentDetailsException(cartId, "Card data is required");
            }
            validateCardData(paymentMethod.getCard(), cartId);
        } else if ("PIX".equalsIgnoreCase(paymentMethodType)) {
            if (paymentMethod.getPixPayer() == null) {
                throw new InvalidPaymentDetailsException(cartId, "PIX payer information (name and CPF) is required");
            }
            if (paymentMethod.getPixPayer().getName() == null || paymentMethod.getPixPayer().getName().isBlank()) {
                throw new InvalidPaymentDetailsException(cartId, "Payer name is required for PIX");
            }
            if (!isValidCpf(paymentMethod.getPixPayer().getCpf())) {
                throw new InvalidPaymentDetailsException(cartId, "Invalid CPF for PIX payer");
            }
        }
    }

    private void validateCardData(CreateAsyncCheckoutCommand.CardData card, String cartId) {
        if (card.getHolderName() == null || card.getHolderName().isBlank()) {
            throw new InvalidPaymentDetailsException(cartId, "Card holder name is required");
        }
        if (card.getNumber() == null || card.getNumber().isBlank()) {
            throw new InvalidPaymentDetailsException(cartId, "Card number is required");
        }
        if (card.getExpiry() == null || card.getExpiry().isBlank()) {
            throw new InvalidPaymentDetailsException(cartId, "Card expiry is required");
        }
        if (card.getCvv() == null || card.getCvv().isBlank()) {
            throw new InvalidPaymentDetailsException(cartId, "Card CVV is required");
        }
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
}