package com.checkout.bff.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${product-service.base-url:http://localhost:8081}")
    private String productServiceBaseUrl;

    @Value("${payment-methods-service.base-url:http://localhost:8083}")
    private String paymentMethodsServiceBaseUrl;

    // RestClient for Product Service
    @Bean
    public RestClient productServiceRestClient() {
        return RestClient.builder()
                .baseUrl(productServiceBaseUrl)
                .build();
    }

    // RestClient for Payment Methods Service
    @Bean
    public RestClient paymentMethodsServiceRestClient() {
        return RestClient.builder()
                .baseUrl(paymentMethodsServiceBaseUrl)
                .build();
    }
}
