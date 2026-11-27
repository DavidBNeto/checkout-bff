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
public class PaymentProcessedEvent {

    @JsonProperty("eventId")
    private String eventId;

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("checkoutId")
    private String checkoutId;

    @JsonProperty("cartId")
    private String cartId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("paymentMethodName")
    private String paymentMethodName;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("result")
    private PaymentResultEvent result;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("correlationId")
    private String correlationId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentResultEvent {
        private String authorizationCode;
        private String maskedCardNumber;
        private String pixCode;
        private String failureReason;
    }
}