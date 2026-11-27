package com.checkout.bff.infrastructure.config;

import com.checkout.bff.application.port.CachePort;
import com.checkout.bff.infrastructure.cache.RedisCacheAdapter;
import com.checkout.bff.infrastructure.client.PaymentMethodsServiceClient;
import com.checkout.bff.infrastructure.client.ProductServiceClient;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.client.RestClient;

@Configuration
@AutoConfiguration 
@PropertySource("classpath:application.properties")
@EnableJpaRepositories(basePackages = "com.checkout.bff.infrastructure.persistence.jpa")
@EntityScan(basePackages = {"com.checkout.bff.infrastructure", "com.checkout.bff.application"})
public class InfrastructureConfig {

    @Bean
    public CachePort cachePort(RedisCacheAdapter redisCacheAdapter) {
        return redisCacheAdapter;
    }

    // External service clients (enabled by default)
    @Bean
    public ProductServiceClient productServiceClient(RestClient restClient,
            @org.springframework.beans.factory.annotation.Value("${product-service.base-url:http://localhost:8081}") String baseUrl,
            @org.springframework.beans.factory.annotation.Value("${product-service.timeout-ms:3000}") int timeoutMs) {
        return new ProductServiceClient(restClient);
    }

    @Bean
    public PaymentMethodsServiceClient paymentMethodsServiceClient(RestClient restClient,
            @org.springframework.beans.factory.annotation.Value("${payment-methods-service.base-url:http://localhost:8083}") String baseUrl,
            @org.springframework.beans.factory.annotation.Value("${payment-methods-service.timeout-ms:3000}") int timeoutMs) {
        return new PaymentMethodsServiceClient(restClient);
    }

}