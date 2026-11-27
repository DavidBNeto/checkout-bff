package com.checkout.bff.infrastructure.messaging.event;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDetailsEvent {

    private String type;
    private String maskedData;
}