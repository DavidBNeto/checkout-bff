package com.checkout.bff.domain.valueobject;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;
import java.util.Currency;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Money {

    private BigDecimal amount;
    private Currency currency;

    public static Money of(BigDecimal amount, String currencyCode) {
        return Money.builder()
                .amount(amount)
                .currency(Currency.getInstance(currencyCode))
                .build();
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return Money.builder()
                .amount(amount)
                .currency(currency)
                .build();
    }

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add amounts with different currencies");
        }
        return Money.of(this.amount.add(other.amount), this.currency);
    }
}