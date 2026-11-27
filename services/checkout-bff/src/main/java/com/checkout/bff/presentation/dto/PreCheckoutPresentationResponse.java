package com.checkout.bff.presentation.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreCheckoutPresentationResponse {

    private UUID cartId;
    private List<ProductDetail> products;
    private List<PaymentMethodDetail> paymentMethods;
    private BigDecimal totalAmount;
    private String currency;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDetail {
        private String slug;
        private String name;
        private String description;
        private BigDecimal price;
        private String currency;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethodDetail {
        private String name;
    }
}