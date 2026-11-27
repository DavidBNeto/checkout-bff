package com.checkout.bff.infrastructure.client;

import com.checkout.bff.domain.entity.Product;
import com.checkout.bff.domain.valueobject.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Disabled("External service clients no longer used - services populate Redis directly")
@ExtendWith(MockitoExtension.class)
class ProductServiceClientTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private ProductServiceClient productServiceClient;

    @BeforeEach
    void setUp() {
        productServiceClient = new ProductServiceClient(restClient);
        // Use reflection to set the baseUrl field
        try {
            java.lang.reflect.Field field = ProductServiceClient.class.getDeclaredField("baseUrl");
            field.setAccessible(true);
            field.set(productServiceClient, "http://localhost:8081");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("findBySlug")
    class FindBySlug {

        @Test
        @DisplayName("Should return empty when slug is null")
        void shouldReturnEmptyWhenSlugIsNull() {
            // When
            Optional<Product> result = productServiceClient.findBySlug(null);

            // Then
            assertThat(result).isEmpty();
            verifyNoInteractions(restClient);
        }

        @Test
        @DisplayName("Should return empty when slug is blank")
        void shouldReturnEmptyWhenSlugIsBlank() {
            // When
            Optional<Product> result = productServiceClient.findBySlug("  ");

            // Then
            assertThat(result).isEmpty();
            verifyNoInteractions(restClient);
        }

        @Test
        @DisplayName("Should return product when found")
        void shouldReturnProductWhenFound() {
            // Given
            String slug = "product-1";
            Product product = createProduct(slug);

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.body(Product.class)).thenReturn(product);

            // When
            Optional<Product> result = productServiceClient.findBySlug(slug);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getSlug()).isEqualTo(slug);
            verify(restClient).get();
        }

        @Test
        @DisplayName("Should return empty when 404 Not Found")
        void shouldReturnEmptyWhen404NotFound() {
            // Given
            String slug = "product-1";
            HttpClientErrorException notFoundEx = HttpClientErrorException.create(
                    HttpStatus.NOT_FOUND, "Not Found", null, null, null);

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.body(Product.class)).thenThrow(notFoundEx);

            // When
            Optional<Product> result = productServiceClient.findBySlug(slug);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception on other errors")
        void shouldThrowExceptionOnOtherErrors() {
            // Given
            String slug = "product-1";
            RuntimeException ex = new RuntimeException("Connection error");

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.body(Product.class)).thenThrow(ex);

            // When/Then
            assertThatThrownBy(() -> productServiceClient.findBySlug(slug))
                    .isInstanceOf(ProductServiceClient.ProductServiceClientException.class)
                    .hasMessageContaining("Failed to fetch product");
        }
    }

    @Nested
    @DisplayName("findBySlugInAndActiveTrue")
    class FindBySlugInAndActiveTrue {

        @Test
        @DisplayName("Should return empty list when slugs is null")
        void shouldReturnEmptyListWhenSlugsIsNull() {
            // When
            List<Product> result = productServiceClient.findBySlugInAndActiveTrue(null);

            // Then
            assertThat(result).isEmpty();
            verifyNoInteractions(restClient);
        }

        @Test
        @DisplayName("Should return empty list when slugs is empty")
        void shouldReturnEmptyListWhenSlugsIsEmpty() {
            // When
            List<Product> result = productServiceClient.findBySlugInAndActiveTrue(Collections.emptyList());

            // Then
            assertThat(result).isEmpty();
            verifyNoInteractions(restClient);
        }

        @Test
        @DisplayName("Should return products when found")
        void shouldReturnProductsWhenFound() {
            // Given
            List<String> slugs = List.of("product-1", "product-2");
            Product[] products = {createProduct("product-1"), createProduct("product-2")};

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.body(Product[].class)).thenReturn(products);

            // When
            List<Product> result = productServiceClient.findBySlugInAndActiveTrue(slugs);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getSlug()).isEqualTo("product-1");
            assertThat(result.get(1).getSlug()).isEqualTo("product-2");
            verify(restClient).get();
        }

        @Test
        @DisplayName("Should return empty list when products array is null")
        void shouldReturnEmptyListWhenProductsArrayIsNull() {
            // Given
            List<String> slugs = List.of("product-1");

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.body(Product[].class)).thenReturn(null);

            // When
            List<Product> result = productServiceClient.findBySlugInAndActiveTrue(slugs);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list when 404 Not Found")
        void shouldReturnEmptyListWhen404NotFound() {
            // Given
            List<String> slugs = List.of("product-1");
            HttpClientErrorException notFoundEx = HttpClientErrorException.create(
                    HttpStatus.NOT_FOUND, "Not Found", null, null, null);

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.body(Product[].class)).thenThrow(notFoundEx);

            // When
            List<Product> result = productServiceClient.findBySlugInAndActiveTrue(slugs);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception on other errors")
        void shouldThrowExceptionOnOtherErrors() {
            // Given
            List<String> slugs = List.of("product-1");
            RuntimeException ex = new RuntimeException("Connection error");

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.body(Product[].class)).thenThrow(ex);

            // When/Then
            assertThatThrownBy(() -> productServiceClient.findBySlugInAndActiveTrue(slugs))
                    .isInstanceOf(ProductServiceClient.ProductServiceClientException.class)
                    .hasMessageContaining("Failed to fetch products");
        }
    }

    @Nested
    @DisplayName("findAllActive")
    class FindAllActive {

        @Test
        @DisplayName("Should return all active products")
        void shouldReturnAllActiveProducts() {
            // Given
            Product[] products = {createProduct("product-1"), createProduct("product-2")};

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri("/api/v1/products?active=true")).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(Product[].class)).thenReturn(products);

            // When
            List<Product> result = productServiceClient.findAllActive();

            // Then
            assertThat(result).hasSize(2);
            verify(restClient).get();
        }

        @Test
        @DisplayName("Should return empty list when products array is null")
        void shouldReturnEmptyListWhenProductsArrayIsNull() {
            // Given
            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri("/api/v1/products?active=true")).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(Product[].class)).thenReturn(null);

            // When
            List<Product> result = productServiceClient.findAllActive();

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list when 404 Not Found")
        void shouldReturnEmptyListWhen404NotFound() {
            // Given
            HttpClientErrorException notFoundEx = HttpClientErrorException.create(
                    HttpStatus.NOT_FOUND, "Not Found", null, null, null);

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri("/api/v1/products?active=true")).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(Product[].class)).thenThrow(notFoundEx);

            // When
            List<Product> result = productServiceClient.findAllActive();

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception on other errors")
        void shouldThrowExceptionOnOtherErrors() {
            // Given
            RuntimeException ex = new RuntimeException("Connection error");

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri("/api/v1/products?active=true")).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(Product[].class)).thenThrow(ex);

            // When/Then
            assertThatThrownBy(() -> productServiceClient.findAllActive())
                    .isInstanceOf(ProductServiceClient.ProductServiceClientException.class)
                    .hasMessageContaining("Failed to fetch all active products");
        }
    }

    private Product createProduct(String slug) {
        Product product = new Product();
        product.setId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        product.setSlug(slug);
        product.setName("Product " + slug);
        product.setPrice(Money.of(new BigDecimal("100.00"), "BRL"));
        product.setActive(true);
        return product;
    }
}