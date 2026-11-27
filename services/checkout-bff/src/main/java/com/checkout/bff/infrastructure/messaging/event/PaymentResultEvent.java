package com.checkout.bff.infrastructure.messaging.event;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResultEvent {

    private String authorizationCode;
    private String maskedCardNumber;
    private String pixCode;
    private String failureReason;
}