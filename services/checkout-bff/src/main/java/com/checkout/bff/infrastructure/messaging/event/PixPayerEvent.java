package com.checkout.bff.infrastructure.messaging.event;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PixPayerEvent {

    private String name; // Payer full name
    private String cpf;  // Payer CPF (Brazilian tax ID)
}