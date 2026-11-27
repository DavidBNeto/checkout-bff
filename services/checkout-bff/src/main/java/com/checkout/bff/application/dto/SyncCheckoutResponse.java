package com.checkout.bff.application.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncCheckoutResponse {

    private UUID cartId;
    private String status; // APPROVED, DECLINED
    private LocalDateTime paidAt;
    private PaymentDetails payment;
    private List<ProductDetail> products;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentDetails {
        private String method; // Cartão de Crédito, Cartão de Débito, PIX
        private String lastDigits; // Card only
        private String pixCode; // PIX only
        private BigDecimal amount;
        private String currency;
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