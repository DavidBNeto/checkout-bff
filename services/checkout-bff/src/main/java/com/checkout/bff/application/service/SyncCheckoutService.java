package com.checkout.bff.application.service;

import com.checkout.bff.application.dto.CreateSyncCheckoutCommand;
import com.checkout.bff.application.dto.SyncCheckoutResponse;
import com.checkout.bff.application.mapper.SyncCheckoutMapper;
import com.checkout.bff.application.port.CachePort;
import com.checkout.bff.application.port.PaymentMethodsServicePort;
import com.checkout.bff.application.usecase.SyncCheckoutUseCase;
import com.checkout.bff.domain.entity.CartPaymentMethod;
import com.checkout.bff.domain.entity.PaymentMethod;
import com.checkout.bff.domain.entity.ShoppingCart;
import com.checkout.bff.domain.exception.CartAlreadyPaidException;
import com.checkout.bff.domain.exception.CartNotFoundException;
import com.checkout.bff.domain.exception.EmptyCartException;
import com.checkout.bff.domain.exception.IdempotencyConflictException;
import com.checkout.bff.domain.exception.InvalidPaymentDetailsException;
import com.checkout.bff.domain.exception.PaymentDeclinedException;
import com.checkout.bff.domain.repository.ShoppingCartRepository;
import br.com.caelum.stella.validation.CPFValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncCheckoutService implements SyncCheckoutUseCase {

    private final ShoppingCartRepository shoppingCartRepository;
    private final PaymentMethodsServicePort paymentMethodsServicePort;
    private final CachePort cachePort;
    private final SyncCheckoutMapper syncCheckoutMapper;

    private static final String IDEMPOTENCY_CACHE_PREFIX = "idempotency:";
    private static final long IDEMPOTENCY_TTL_MINUTES = 30;

    @Override
    @Transactional
    public SyncCheckoutResponse execute(CreateSyncCheckoutCommand command, String idempotencyKey) {
        log.info("Processing sync checkout for cart: {}", command.getCartId());

        // Check idempotency key if provided
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            checkIdempotency(idempotencyKey);
        }

        // Retrieve cart with items and available payment methods
        ShoppingCart cart = shoppingCartRepository.findByPublicIdWithItems(command.getCartId())
                .orElseThrow(() -> new CartNotFoundException(command.getCartId().toString()));

        // Validate cart state
        validateCart(cart);

        // Validate payment method matches what was offered
        validatePaymentMethod(cart, command.getPaymentMethod());

        // Process payment simulation
        boolean approved = processPayment(cart, command.getPaymentMethod());

        if (!approved) {
            // Record payment attempt (cart remains PENDING)
            log.warn("Payment declined for cart: {}", cart.getPublicId());
            throw new PaymentDeclinedException(
                    cart.getPublicId().toString(),
                    command.getPaymentMethod().getType(),
                    "Payment simulation declined"
            );
        }

        // Mark cart as PAID
        cart.setStatus("PAID");
        cart.setPaidAt(LocalDateTime.now());
        ShoppingCart savedCart = shoppingCartRepository.save(cart);

        // Invalidate cart cache
        cachePort.delete("cart:" + savedCart.getPublicId());

        // Store idempotency key to prevent duplicate processing
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            cachePort.set(IDEMPOTENCY_CACHE_PREFIX + idempotencyKey, savedCart.getPublicId().toString(),
                    IDEMPOTENCY_TTL_MINUTES, TimeUnit.MINUTES);
        }

        // Map to response
        return syncCheckoutMapper.toResponse(savedCart, command.getPaymentMethod());
    }

    private void checkIdempotency(String idempotencyKey) {
        Optional<String> cached = cachePort.get(IDEMPOTENCY_CACHE_PREFIX + idempotencyKey, String.class);
        if (cached.isPresent()) {
            log.warn("Idempotency conflict for key: {}", idempotencyKey);
            throw new IdempotencyConflictException(idempotencyKey);
        }
    }

    private void validateCart(ShoppingCart cart) {
        // Check cart status
        if (cart.isPaid()) {
            log.warn("Attempt to checkout already paid cart: {}", cart.getPublicId());
            throw new CartAlreadyPaidException(cart.getPublicId().toString());
        }

        if (cart.isExpired()) {
            log.warn("Attempt to checkout expired cart: {}", cart.getPublicId());
            throw new CartNotFoundException(cart.getPublicId().toString());
        }

        if (cart.isCancelled()) {
            log.warn("Attempt to checkout cancelled cart: {}", cart.getPublicId());
            throw new CartNotFoundException(cart.getPublicId().toString());
        }

        // Check cart has items
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            log.warn("Cart has no items: {}", cart.getPublicId());
            throw new EmptyCartException();
        }
    }

    private void validatePaymentMethod(ShoppingCart cart, CreateSyncCheckoutCommand.PaymentMethodData paymentMethod) {
        if (paymentMethod.getType() == null) {
            throw new InvalidPaymentDetailsException(cart.getPublicId().toString(), "Payment method type is required");
        }

        // Check if payment method type was offered during pre-checkout
        // The cart's availablePaymentMethods has the payment methods that were available
        String requestedType = paymentMethod.getType().toUpperCase();

        boolean methodOffered = cart.getAvailablePaymentMethods().stream()
                .map(CartPaymentMethod::getPaymentMethodName)
                .map(this::mapPaymentMethodNameToType)
                .anyMatch(offeredType -> offeredType.equalsIgnoreCase(requestedType));

        if (!methodOffered) {
            log.warn("Payment method {} not offered for cart: {}! Offered types: {}", paymentMethod.getType(), cart.getPublicId(), cart.getAvailablePaymentMethods().stream()
                    .map(CartPaymentMethod::getPaymentMethodName)
                    .map(this::mapPaymentMethodNameToType)
                    .toList());
            throw new InvalidPaymentDetailsException(
                    cart.getPublicId().toString(),
                    "Payment method not offered during pre-checkout"
            );
        }

        // Validate card data for card payments
        if ("CREDIT_CARD".equalsIgnoreCase(paymentMethod.getType())
                || "DEBIT_CARD".equalsIgnoreCase(paymentMethod.getType())) {
            if (paymentMethod.getCard() == null) {
                throw new InvalidPaymentDetailsException(cart.getPublicId().toString(), "Card data is required");
            }
            validateCardData(paymentMethod.getCard(), cart.getPublicId().toString());
        }

        // Validate PIX payer for PIX payments
        if ("PIX".equalsIgnoreCase(paymentMethod.getType())) {
            if (paymentMethod.getPixPayer() == null) {
                throw new InvalidPaymentDetailsException(cart.getPublicId().toString(), "PIX payer information (name and CPF) is required");
            }
            if (paymentMethod.getPixPayer().getName() == null || paymentMethod.getPixPayer().getName().isBlank()) {
                throw new InvalidPaymentDetailsException(cart.getPublicId().toString(), "Payer name is required for PIX");
            }
            if (!isValidCpf(paymentMethod.getPixPayer().getCpf())) {
                throw new InvalidPaymentDetailsException(cart.getPublicId().toString(), "Invalid CPF for PIX payer");
            }
        }
    }

    private void validateCardData(CreateSyncCheckoutCommand.CardData card, String cartId) {
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

    private boolean processPayment(ShoppingCart cart, CreateSyncCheckoutCommand.PaymentMethodData paymentMethod) {
        String type = paymentMethod.getType().toUpperCase();

        if ("CREDIT_CARD".equals(type) || "DEBIT_CARD".equals(type)) {
            return processCardPayment(paymentMethod.getCard());
        } else if ("PIX".equals(type)) {
            return processPixPayment(paymentMethod.getPixPayer());
        }

        throw new IllegalArgumentException("Unsupported payment method type: " + type);
    }

    private boolean processCardPayment(CreateSyncCheckoutCommand.CardData card) {
        String number = card.getNumber().replaceAll("\\s", "");

        // Rule: Card ending in 0002 is always declined
        if (number.endsWith("0002")) {
            log.info("Card ending in 0002 - declined (insufficient funds simulation)");
            return false;
        }

        // Rule: Card ending in 0001 is always approved
        if (number.endsWith("0001")) {
            log.info("Card ending in 0001 - approved (successful payment simulation)");
            return true;
        }

        return true;
    }

    private boolean processPixPayment(CreateSyncCheckoutCommand.PixPayerData pixPayer) {
        // For PIX, we already validated the CPF before this point
        // Simulate PIX payment approval
        log.info("PIX payment approved for payer: {}", pixPayer.getName());
        return true;
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

    private String mapPaymentMethodNameToType(String name) {
        // Map from payment method name to type
        return switch (name.toLowerCase()) {
            case "credit-card", "cartao de credito", "cartão de crédito" -> "CREDIT_CARD";
            case "debit-card", "cartao de debito", "cartão de débito" -> "DEBIT_CARD";
            case "pix" -> "PIX";
            default -> name.toUpperCase();
        };
    }
}