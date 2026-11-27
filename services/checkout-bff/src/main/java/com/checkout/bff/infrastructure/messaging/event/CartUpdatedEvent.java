package com.checkout.bff.infrastructure.messaging.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartUpdatedEvent {

    private String eventId;
    private String eventType;
    private Long cartId;
    private String cartStatus;
    private BigDecimal totalAmount;
    private String currency;
    private List<CartItemEvent> items;
    private Instant timestamp;
    private String correlationId;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemEvent {
        private Long productId;
        private String productSlug;
        private String productName;
        private BigDecimal unitPrice;
        private Integer quantity;
    }
}