package com.checkout.product.domain.valueobject;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Money implements Serializable {

    private BigDecimal amount;
    private Currency currency;

    public static Money of(BigDecimal amount, Currency currency) {
        return Money.builder().amount(amount).currency(currency).build();
    }

    public static Money of(String amount, String currency) {
        return Money.builder()
                .amount(new BigDecimal(amount))
                .currency(Currency.getInstance(currency))
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return Objects.equals(amount, money.amount) && Objects.equals(currency, money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return amount + " " + currency.getCurrencyCode();
    }
}