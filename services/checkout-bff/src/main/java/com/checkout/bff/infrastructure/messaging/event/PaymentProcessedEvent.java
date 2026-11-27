package com.checkout.bff.infrastructure.messaging.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessedEvent {

    private String eventId;
    private String eventType;
    private String checkoutId;
    private String cartId;
    private String status;
    private String paymentMethodName;
    private BigDecimal amount;
    private String currency;
    private PaymentResultEvent result;
    private Instant timestamp;
    private String correlationId;
}