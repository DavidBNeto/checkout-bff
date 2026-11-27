package com.checkout.bff.application.service;

import com.checkout.bff.application.dto.CreatePreCheckoutCommand;
import com.checkout.bff.application.dto.PreCheckoutResponse;
import com.checkout.bff.application.mapper.PreCheckoutMapper;
import com.checkout.bff.application.port.CachePort;
import com.checkout.bff.application.port.ProductServicePort;
import com.checkout.bff.application.port.PaymentMethodsServicePort;
import com.checkout.bff.domain.entity.PaymentMethod;
import com.checkout.bff.domain.entity.Product;
import com.checkout.bff.domain.entity.ShoppingCart;
import com.checkout.bff.domain.exception.EmptyCartException;
import com.checkout.bff.domain.exception.ProductNotFoundException;
import com.checkout.bff.domain.exception.ProductUnavailableException;
import com.checkout.bff.domain.repository.ShoppingCartRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatePreCheckoutServiceTest {

    @Mock
    private ProductServicePort productServicePort;
    @Mock
    private PaymentMethodsServicePort paymentMethodsServicePort;
    @Mock
    private ShoppingCartRepository shoppingCartRepository;
    @Mock
    private CachePort cachePort;
    @Mock
    private PreCheckoutMapper preCheckoutMapper;

    @Captor
    private ArgumentCaptor<ShoppingCart> cartCaptor;

    private CreatePreCheckoutService createPreCheckoutService;

    @BeforeEach
    void setUp() {
        createPreCheckoutService = new CreatePreCheckoutService(
                productServicePort,
                paymentMethodsServicePort,
                shoppingCartRepository,
                cachePort,
                preCheckoutMapper
        );
    }

    @Nested
    @DisplayName("Execute Pre-Checkout")
    class ExecutePreCheckout {

        @Test
        @DisplayName("Should create pre-checkout successfully with valid products")
        void shouldCreatePreCheckoutSuccessfullyWithValidProducts() {
            // Given
            String slug1 = "product-1";
            String slug2 = "product-2";
            CreatePreCheckoutCommand command = createCommand(slug1, slug2);

            Product product1 = createProduct(slug1, "Product 1", BigDecimal.valueOf(50.00));
            Product product2 = createProduct(slug2, "Product 2", BigDecimal.valueOf(75.00));
            List<Product> products = List.of(product1, product2);

            PaymentMethod paymentMethod1 = createPaymentMethod("Cartão de Crédito", true);
            PaymentMethod paymentMethod2 = createPaymentMethod("PIX", true);
            List<PaymentMethod> paymentMethods = List.of(paymentMethod1, paymentMethod2);

            ShoppingCart savedCart = createShoppingCart(UUID.randomUUID());

            when(cachePort.getTyped(eq("products:active"), any(TypeReference.class))).thenReturn(Optional.empty());
            when(cachePort.getTyped(eq("payment-methods:active"), any(TypeReference.class))).thenReturn(Optional.empty());
            when(productServicePort.findBySlugInAndActiveTrue(anyList())).thenReturn(products);
            when(productServicePort.findAllActive()).thenReturn(products);
            when(paymentMethodsServicePort.findAllActive()).thenReturn(paymentMethods);
            when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(savedCart);
            when(preCheckoutMapper.toResponse(any(ShoppingCart.class), eq(products), eq(paymentMethods))).thenReturn(createPreCheckoutResponse(savedCart));

            // When
            PreCheckoutResponse response = createPreCheckoutService.execute(command);

            // Then
            assertThat(response).isNotNull();
            verify(cachePort).set(eq("products:active"), any(), anyLong(), any());
            verify(cachePort).set(eq("payment-methods:active"), any(), anyLong(), any());
            verify(shoppingCartRepository).save(cartCaptor.capture());
            // Cart caching is currently commented out in the service
            // verify(cachePort).set(eq("cart:" + savedCart.getPublicId()), eq(savedCart), anyLong(), any());

            ShoppingCart capturedCart = cartCaptor.getValue();
            assertThat(capturedCart.getStatus()).isEqualTo("PENDING");
            assertThat(capturedCart.getItems()).hasSize(2);
            assertThat(capturedCart.getAvailablePaymentMethods()).isNotEmpty();
            assertThat(capturedCart.getAvailablePaymentMethods()).hasSize(2);
        }

        @Test
        @DisplayName("Should throw EmptyCartException when no products provided")
        void shouldThrowEmptyCartExceptionWhenNoProductsProvided() {
            // Given
            CreatePreCheckoutCommand command = CreatePreCheckoutCommand.builder()
                    .products(List.of())
                    .build();

            // When / Then
            assertThatThrownBy(() -> createPreCheckoutService.execute(command))
                    .isInstanceOf(EmptyCartException.class);

            verifyNoInteractions(productServicePort, paymentMethodsServicePort, shoppingCartRepository);
        }

        @Test
        @DisplayName("Should throw ProductNotFoundException when product not found")
        void shouldThrowProductNotFoundExceptionWhenProductNotFound() {
            // Given
            CreatePreCheckoutCommand command = createCommand("product-1", "product-2");

            Product product1 = createProduct("product-1", "Product 1", BigDecimal.valueOf(50.00));
            // Only one product found
            List<Product> products = List.of(product1);

            when(cachePort.getTyped(eq("products:active"), any(TypeReference.class))).thenReturn(Optional.empty());
            when(productServicePort.findBySlugInAndActiveTrue(anyList())).thenReturn(products);

            // When / Then
            assertThatThrownBy(() -> createPreCheckoutService.execute(command))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessageContaining("product-2");
        }

        @Test
        @DisplayName("Should throw ProductUnavailableException when product is inactive")
        void shouldThrowProductUnavailableExceptionWhenProductIsInactive() {
            // Given
            CreatePreCheckoutCommand command = createCommand("product-1");

            Product inactiveProduct = createProduct("product-1", "Product 1", BigDecimal.valueOf(50.00));
            inactiveProduct.setActive(false);
            List<Product> products = List.of(inactiveProduct);

            when(cachePort.getTyped(eq("products:active"), any(TypeReference.class))).thenReturn(Optional.empty());
            when(productServicePort.findBySlugInAndActiveTrue(anyList())).thenReturn(products);

            // When / Then
            assertThatThrownBy(() -> createPreCheckoutService.execute(command))
                    .isInstanceOf(ProductUnavailableException.class)
                    .hasMessageContaining("product-1");
        }

        @Test
        @DisplayName("Should use cached products when available")
        void shouldUseCachedProductsWhenAvailable() {
            // Given
            CreatePreCheckoutCommand command = createCommand("product-1");
            Product product1 = createProduct("product-1", "Product 1", BigDecimal.valueOf(50.00));
            List<Product> cachedProducts = List.of(product1);
            PaymentMethod paymentMethod = createPaymentMethod("Cartão de Crédito", true);
            List<PaymentMethod> paymentMethods = List.of(paymentMethod);
            ShoppingCart savedCart = createShoppingCart(UUID.randomUUID());

            when(cachePort.getTyped(eq("products:active"), any(TypeReference.class))).thenReturn(Optional.of(cachedProducts));
            when(cachePort.getTyped(eq("payment-methods:active"), any(TypeReference.class))).thenReturn(Optional.empty());
            when(paymentMethodsServicePort.findAllActive()).thenReturn(paymentMethods);
            when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(savedCart);
            when(preCheckoutMapper.toResponse(any(ShoppingCart.class), eq(cachedProducts), eq(paymentMethods))).thenReturn(createPreCheckoutResponse(savedCart));

            // When
            createPreCheckoutService.execute(command);

            // Then
            verify(productServicePort, never()).findBySlugInAndActiveTrue(anyList());
            verify(productServicePort, never()).findAllActive();
        }

        @Test
        @DisplayName("Should use cached payment methods when available")
        void shouldUseCachedPaymentMethodsWhenAvailable() {
            // Given
            CreatePreCheckoutCommand command = createCommand("product-1");
            Product product1 = createProduct("product-1", "Product 1", BigDecimal.valueOf(50.00));
            List<Product> products = List.of(product1);
            PaymentMethod paymentMethod = createPaymentMethod("Cartão de Crédito", true);
            List<PaymentMethod> cachedPaymentMethods = List.of(paymentMethod);
            ShoppingCart savedCart = createShoppingCart(UUID.randomUUID());

            when(cachePort.getTyped(eq("products:active"), any(TypeReference.class))).thenReturn(Optional.empty());
            when(productServicePort.findBySlugInAndActiveTrue(anyList())).thenReturn(products);
            when(productServicePort.findAllActive()).thenReturn(products);
            when(cachePort.getTyped(eq("payment-methods:active"), any(TypeReference.class))).thenReturn(Optional.of(cachedPaymentMethods));
            when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(savedCart);
            when(preCheckoutMapper.toResponse(any(ShoppingCart.class), eq(products), eq(cachedPaymentMethods))).thenReturn(createPreCheckoutResponse(savedCart));

            // When
            createPreCheckoutService.execute(command);

            // Then
            verify(paymentMethodsServicePort, never()).findAllActive();
        }
    }

    // Helper methods
    private CreatePreCheckoutCommand createCommand(String... slugs) {
        List<CreatePreCheckoutCommand.ProductSelection> products = java.util.Arrays.stream(slugs)
                .map(slug -> CreatePreCheckoutCommand.ProductSelection.builder().slug(slug).build())
                .toList();
        return CreatePreCheckoutCommand.builder().products(products).build();
    }

    private Product createProduct(String slug, String name, BigDecimal price) {
        Product product = new Product();
        product.setId(1L);
        product.setSlug(slug);
        product.setName(name);
        product.setPrice(com.checkout.bff.domain.valueobject.Money.of(price, "BRL"));
        product.setActive(true);
        return product;
    }

    private PaymentMethod createPaymentMethod(String name, boolean active) {
        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setId(1L);
        paymentMethod.setName(name);
        paymentMethod.setActive(active);
        return paymentMethod;
    }

    private ShoppingCart createShoppingCart(UUID publicId) {
        ShoppingCart cart = new ShoppingCart();
        cart.setId(1L);
        cart.setPublicId(publicId);
        cart.setStatus("PENDING");
        return cart;
    }

    private PreCheckoutResponse createPreCheckoutResponse(ShoppingCart cart) {
        return PreCheckoutResponse.builder()
                .cartId(cart.getPublicId())
                .products(List.of())
                .paymentMethods(List.of())
                .build();
    }
}