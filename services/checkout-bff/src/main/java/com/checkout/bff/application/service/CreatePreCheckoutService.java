package com.checkout.bff.application.service;

import com.checkout.bff.application.dto.CreatePreCheckoutCommand;
import com.checkout.bff.application.dto.PreCheckoutResponse;
import com.checkout.bff.application.mapper.PreCheckoutMapper;
import com.checkout.bff.application.port.CachePort;
import com.checkout.bff.application.port.ProductServicePort;
import com.checkout.bff.application.port.PaymentMethodsServicePort;
import com.checkout.bff.application.usecase.CreatePreCheckoutUseCase;
import com.checkout.bff.domain.entity.CartPaymentMethod;
import com.checkout.bff.domain.entity.PaymentMethod;
import com.checkout.bff.domain.entity.Product;
import com.checkout.bff.domain.entity.ShoppingCart;
import com.checkout.bff.domain.exception.EmptyCartException;
import com.checkout.bff.domain.exception.ProductNotFoundException;
import com.checkout.bff.domain.exception.ProductUnavailableException;
import com.checkout.bff.domain.repository.ShoppingCartRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreatePreCheckoutService implements CreatePreCheckoutUseCase {

    private final ProductServicePort productServicePort;
    private final PaymentMethodsServicePort paymentMethodsServicePort;
    private final ShoppingCartRepository shoppingCartRepository;
    private final CachePort cachePort;
    private final PreCheckoutMapper preCheckoutMapper;

    private static final String PRODUCTS_CACHE_KEY = "products:active";
    private static final String PAYMENT_METHODS_CACHE_KEY = "payment-methods:active";
    private static final TypeReference<List<Product>> PRODUCT_LIST_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<PaymentMethod>> PAYMENT_METHOD_LIST_TYPE = new TypeReference<>() {};

    @Override
    @Transactional
    public PreCheckoutResponse execute(CreatePreCheckoutCommand command) {
        log.info("Creating pre-checkout for {} products", command.getProducts().size());

        // Extract unique slugs from request
        List<String> requestedSlugs = command.getProducts().stream()
                .map(CreatePreCheckoutCommand.ProductSelection::getSlug)
                .distinct()
                .toList();

        // Validate non-empty
        if (requestedSlugs.isEmpty()) {
            throw new EmptyCartException();
        }

        // Retrieve products (with caching)
        List<Product> products = retrieveProducts(requestedSlugs);

        // Validate all products exist and are active
        validateProducts(requestedSlugs, products);

        // Retrieve payment methods (with caching)
        List<PaymentMethod> paymentMethods = retrievePaymentMethods();

        // Calculate total amount
        BigDecimal totalAmount = calculateTotalAmount(products);

        // Build and persist cart
        ShoppingCart cart = buildCart(products, paymentMethods, totalAmount);
        // ShoppingCart savedCart = shoppingCartRepository.save(cart);

        // Cache cart response
        // cachePort.set("cart:" + savedCart.getPublicId(), savedCart, 30, java.util.concurrent.TimeUnit.MINUTES);

        // Map to response
        return preCheckoutMapper.toResponse(cart, products, paymentMethods);
    }

    private List<Product> retrieveProducts(List<String> slugs) {
        log.debug("Retrieving products for slugs: {}", slugs);

        // Try cache first using TypeReference for proper generic deserialization
        List<Product> cachedProducts = cachePort.getTyped(PRODUCTS_CACHE_KEY, PRODUCT_LIST_TYPE).orElse(null);

        if (cachedProducts != null && !cachedProducts.isEmpty()) {
            log.debug("Cache hit for products");
            return cachedProducts.stream()
                    .filter(p -> slugs.contains(p.getSlug()))
                    .toList();
        }

        // Cache miss - fetch from service
        log.debug("Cache miss for products, fetching from service");
        List<Product> products = productServicePort.findBySlugInAndActiveTrue(slugs);

        // Cache all active products for future requests
        List<Product> allActiveProducts = productServicePort.findAllActive();
        cachePort.set(PRODUCTS_CACHE_KEY, allActiveProducts, 10, java.util.concurrent.TimeUnit.MINUTES);

        return products;
    }

    private void validateProducts(List<String> requestedSlugs, List<Product> foundProducts) {
        Set<String> foundSlugs = foundProducts.stream()
                .map(Product::getSlug)
                .collect(Collectors.toSet());

        List<String> notFound = requestedSlugs.stream()
                .filter(slug -> !foundSlugs.contains(slug))
                .toList();

        if (!notFound.isEmpty()) {
            log.warn("Products not found: {}", notFound);
            throw new ProductNotFoundException(String.join(", ", notFound));
        }

        // Check for inactive products
        List<String> inactive = foundProducts.stream()
                .filter(p -> !p.isActive())
                .map(Product::getSlug)
                .toList();

        if (!inactive.isEmpty()) {
            log.warn("Products inactive: {}", inactive);
            throw new ProductUnavailableException(String.join(", ", inactive));
        }
    }

    private List<PaymentMethod> retrievePaymentMethods() {
        log.debug("Retrieving payment methods");

        // Try cache first using TypeReference for proper generic deserialization
        List<PaymentMethod> cachedMethods = cachePort.getTyped(PAYMENT_METHODS_CACHE_KEY, PAYMENT_METHOD_LIST_TYPE).orElse(null);

        if (cachedMethods != null && !cachedMethods.isEmpty()) {
            log.debug("Cache hit for payment methods");
            return cachedMethods;
        }

        // Cache miss - fetch from service
        log.debug("Cache miss for payment methods, fetching from service");
        List<PaymentMethod> methods = paymentMethodsServicePort.findAllActive();

        cachePort.set(PAYMENT_METHODS_CACHE_KEY, methods, 15, java.util.concurrent.TimeUnit.MINUTES);

        return methods;
    }

    private BigDecimal calculateTotalAmount(List<Product> products) {
        return products.stream()
                .map(p -> p.getPrice().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ShoppingCart buildCart(List<Product> products,
                                   List<PaymentMethod> paymentMethods,
                                   BigDecimal totalAmount) {
        ShoppingCart cart = ShoppingCart.builder()
                .publicId(UUID.randomUUID())
                .price(com.checkout.bff.domain.valueobject.Money.of(totalAmount, "BRL"))
                .status("PENDING")
                .build();

        // Add cart items
        for (Product product : products) {
            cart.addItem(com.checkout.bff.domain.entity.CartItem.builder()
                    .product(product)
                    .quantity(1)
                    .build());
        }

        shoppingCartRepository.save(cart);

        // Store available payment methods in intermediary table (for checkout validation)
        if (!paymentMethods.isEmpty()) {
            for (PaymentMethod paymentMethod : paymentMethods) {
                CartPaymentMethod cartPaymentMethod = CartPaymentMethod.builder()
                        .cartId(cart.getId())
                        .publicCartId(cart.getPublicId())
                        .paymentMethod(paymentMethod)
                        .paymentMethodName(paymentMethod.getName())
                        .build();
                cart.addAvailablePaymentMethod(cartPaymentMethod);
            }
        }

        return cart;
    }
}