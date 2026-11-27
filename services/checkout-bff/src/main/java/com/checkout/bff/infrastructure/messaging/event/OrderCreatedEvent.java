package com.checkout.bff.infrastructure.messaging.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {

    private String eventId;
    private String eventType;
    private String cartId;
    private String checkoutId;
    private Long paymentMethodId;
    private String paymentMethodName;
    private BigDecimal amount;
    private String currency;
    private PaymentDetailsEvent paymentDetails;
    private PixPayerEvent pixPayer; // PIX payer info (name and CPF)
    private String cardPayerCpf; // CPF for card payments
    private Instant timestamp;
    private String correlationId;
}