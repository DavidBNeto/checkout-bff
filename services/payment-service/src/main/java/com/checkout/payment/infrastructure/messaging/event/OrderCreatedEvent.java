package com.checkout.payment.infrastructure.messaging.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {

    @JsonProperty("eventId")
    private String eventId;

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("checkoutId")
    private String checkoutId;

    @JsonProperty("cartId")
    private String cartId;

    @JsonProperty("correlationId")
    private String correlationId;

    @JsonProperty("paymentMethodId")
    private Long paymentMethodId;

    @JsonProperty("paymentMethodName")
    private String paymentMethodName;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("paymentDetails")
    private PaymentDetailsEvent paymentDetails;

    @JsonProperty("pixPayer")
    private PixPayerEvent pixPayer; // PIX payer info (name and CPF)

    @JsonProperty("cardPayerCpf")
    private String cardPayerCpf; // CPF for card payments

    @JsonProperty("timestamp")
    private Instant timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentDetailsEvent {
        private String type;
        private String maskedData;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PixPayerEvent {
        private String name; // Payer full name
        private String cpf;  // Payer CPF (Brazilian tax ID)
    }
}