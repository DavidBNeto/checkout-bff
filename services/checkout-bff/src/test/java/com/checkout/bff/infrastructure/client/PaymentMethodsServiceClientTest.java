package com.checkout.bff.infrastructure.client;

import com.checkout.bff.domain.entity.PaymentMethod;
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

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Disabled("External service clients no longer used - services populate Redis directly")
@ExtendWith(MockitoExtension.class)
class PaymentMethodsServiceClientTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private PaymentMethodsServiceClient paymentMethodsServiceClient;

    @BeforeEach
    void setUp() {
        paymentMethodsServiceClient = new PaymentMethodsServiceClient(restClient);
        // Use reflection to set the baseUrl field
        try {
            java.lang.reflect.Field field = PaymentMethodsServiceClient.class.getDeclaredField("baseUrl");
            field.setAccessible(true);
            field.set(paymentMethodsServiceClient, "http://localhost:8082");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("findAllActive")
    class FindAllActive {

        @Test
        @DisplayName("Should return active payment methods")
        void shouldReturnActivePaymentMethods() {
            // Given
            PaymentMethod[] paymentMethods = {createPaymentMethod("Cartão de Crédito"), createPaymentMethod("PIX")};

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri("/api/v1/payment-methods?active=true")).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.body(PaymentMethod[].class)).thenReturn(paymentMethods);

            // When
            List<PaymentMethod> result = paymentMethodsServiceClient.findAllActive();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Cartão de Crédito");
            assertThat(result.get(1).getName()).isEqualTo("PIX");
            verify(restClient).get();
        }

        @Test
        @DisplayName("Should return empty list when payment methods array is null")
        void shouldReturnEmptyListWhenPaymentMethodsArrayIsNull() {
            // Given
            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri("/api/v1/payment-methods?active=true")).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.body(PaymentMethod[].class)).thenReturn(null);

            // When
            List<PaymentMethod> result = paymentMethodsServiceClient.findAllActive();

            // Then
            assertThat(result).isEmpty();
        }

//        @Test
//        @DisplayName("Should return empty list when 404 Not Found")
//        void shouldReturnEmptyListWhen404NotFound() {
//            // Given
//            HttpClientErrorException notFoundEx = new HttpClientErrorException(
//                    "Not Found", HttpStatus.NOT_FOUND, null, null, null);
//
//            when(restClient.get()).thenReturn(requestHeadersUriSpec);
//            when(requestHeadersUriSpec.uri("/api/v1/payment-methods?active=true")).thenReturn(requestBodySpec);
//            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
//            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
//            when(responseSpec.body(PaymentMethod[].class)).thenThrow(notFoundEx);
//
//            // When
//            List<PaymentMethod> result = paymentMethodsServiceClient.findAllActive();
//
//            // Then
//            assertThat(result).isEmpty();
//        }

        @Test
        @DisplayName("Should throw exception on other errors")
        void shouldThrowExceptionOnOtherErrors() {
            // Given
            RuntimeException ex = new RuntimeException("Connection error");

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri("/api/v1/payment-methods?active=true")).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.body(PaymentMethod[].class)).thenThrow(ex);

            // When/Then
            assertThatThrownBy(() -> paymentMethodsServiceClient.findAllActive())
                    .isInstanceOf(PaymentMethodsServiceClient.PaymentMethodsServiceClientException.class)
                    .hasMessageContaining("Failed to fetch active payment methods");
        }
    }

    private PaymentMethod createPaymentMethod(String name) {
        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setId(1L);
        paymentMethod.setName(name);
        paymentMethod.setActive(true);
        return paymentMethod;
    }
}