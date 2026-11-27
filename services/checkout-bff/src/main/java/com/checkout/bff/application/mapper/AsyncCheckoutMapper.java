package com.checkout.bff.application.mapper;

import com.checkout.bff.application.dto.AsyncCheckoutResponse;
import com.checkout.bff.domain.entity.CheckoutOperation;
import com.checkout.bff.domain.entity.ShoppingCart;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AsyncCheckoutMapper {

    public AsyncCheckoutResponse toResponse(CheckoutOperation operation) {
        return AsyncCheckoutResponse.builder()
                .checkoutId(operation.getPublicId())
                .cartId(operation.getCart().getPublicId())
                .status(operation.getStatus())
                .createdAt(operation.getCreatedAt())
                .processedAt(operation.getProcessedAt())
                .payment(mapPaymentDetails(operation))
                .products(mapProducts(operation.getCart()))
                .build();
    }

    private AsyncCheckoutResponse.PaymentDetails mapPaymentDetails(CheckoutOperation operation) {
        AsyncCheckoutResponse.PaymentDetails details = AsyncCheckoutResponse.PaymentDetails.builder()
                .method(operation.getPaymentMethodName())
                .amount(operation.getAmount())
                .currency(operation.getCurrency())
                .build();

        if (operation.getMaskedCardNumber() != null) {
            details.setLastDigits(operation.getMaskedCardNumber());
        }
        if (operation.getPixCode() != null) {
            details.setPixCode(operation.getPixCode());
        }
        if (operation.getAuthorizationCode() != null) {
            details.setAuthorizationCode(operation.getAuthorizationCode());
        }
        if (operation.getFailureReason() != null) {
            details.setFailureReason(operation.getFailureReason());
        }

        return details;
    }

    private List<AsyncCheckoutResponse.ProductDetail> mapProducts(ShoppingCart cart) {
        return cart.getItems().stream()
                .map(item -> AsyncCheckoutResponse.ProductDetail.builder()
                        .slug(item.getProduct().getSlug())
                        .name(item.getProduct().getName())
                        .price(item.getProduct().getPrice().getAmount())
                        .build())
                .toList();
    }
}