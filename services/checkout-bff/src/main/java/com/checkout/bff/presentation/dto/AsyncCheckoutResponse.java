package com.checkout.bff.presentation.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsyncCheckoutResponse {

    private UUID checkoutId;
    private UUID cartId;
    private String status; // PENDING, PROCESSING, APPROVED, DECLINED, FAILED
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private PaymentDetails payment;
    private List<ProductDetail> products;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentDetails {
        private String method; // Cartão de Crédito, Cartão de Débito, PIX
        private String lastDigits; // For card payments
        private String pixCode; // For PIX payments
        private BigDecimal amount;
        private String currency;
        private String authorizationCode;
        private String failureReason;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDetail {
        private String slug;
        private String name;
        private BigDecimal price;
    }
}