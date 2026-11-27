package com.checkout.bff.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncCheckoutRequest {

    @NotNull(message = "Cart ID is required")
    private UUID cartId;

    @NotNull(message = "Payment method is required")
    @Valid
    private PaymentMethodData paymentMethod;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethodData {
        @NotNull(message = "Payment method type is required")
        @Pattern(regexp = "^(DEBIT_CARD|CREDIT_CARD|PIX)$", message = "Payment method must be DEBIT_CARD, CREDIT_CARD, or PIX")
        private String type;

        @Valid
        private CardData card;

        @Valid
        private PixPayerData pixPayer;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardData {
        @NotNull(message = "Card holder name is required")
        @Size(min = 1, max = 255, message = "Holder name must be between 1 and 255 characters")
        private String holderName;

        @NotNull(message = "Card number is required")
        @Pattern(regexp = "^\\d{13,19}$", message = "Card number must be 13-19 digits")
        private String number;

        @NotNull(message = "Card expiry is required")
        @Pattern(regexp = "^(0[1-9]|1[0-2])\\/\\d{2}$", message = "Expiry must be in MM/YY format")
        private String expiry;

        @NotNull(message = "Card CVV is required")
        @Pattern(regexp = "^\\d{3,4}$", message = "CVV must be 3-4 digits")
        private String cvv;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PixPayerData {
        @NotNull(message = "Payer name is required for PIX")
        @Size(min = 1, max = 255, message = "Payer name must be between 1 and 255 characters")
        private String name;

        @NotNull(message = "Payer CPF is required for PIX")
        @Pattern(regexp = "^\\d{11}$", message = "CPF must be 11 digits")
        private String cpf;
    }
}