package com.checkout.bff.presentation.mapper;

import com.checkout.bff.application.dto.AsyncCheckoutResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AsyncCheckoutPresentationMapper {

    public com.checkout.bff.presentation.dto.AsyncCheckoutResponse toResponse(
            AsyncCheckoutResponse applicationResponse) {
        return com.checkout.bff.presentation.dto.AsyncCheckoutResponse.builder()
                .checkoutId(applicationResponse.getCheckoutId())
                .cartId(applicationResponse.getCartId())
                .status(applicationResponse.getStatus())
                .createdAt(applicationResponse.getCreatedAt())
                .processedAt(applicationResponse.getProcessedAt())
                .payment(mapPaymentDetails(applicationResponse.getPayment()))
                .products(mapProducts(applicationResponse.getProducts()))
                .build();
    }

    private com.checkout.bff.presentation.dto.AsyncCheckoutResponse.PaymentDetails mapPaymentDetails(
            AsyncCheckoutResponse.PaymentDetails payment) {
        return com.checkout.bff.presentation.dto.AsyncCheckoutResponse.PaymentDetails.builder()
                .method(payment.getMethod())
                .lastDigits(payment.getLastDigits())
                .pixCode(payment.getPixCode())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .authorizationCode(payment.getAuthorizationCode())
                .failureReason(payment.getFailureReason())
                .build();
    }

    private List<com.checkout.bff.presentation.dto.AsyncCheckoutResponse.ProductDetail> mapProducts(
            List<AsyncCheckoutResponse.ProductDetail> products) {
        return products.stream()
                .map(p -> com.checkout.bff.presentation.dto.AsyncCheckoutResponse.ProductDetail.builder()
                        .slug(p.getSlug())
                        .name(p.getName())
                        .price(p.getPrice())
                        .build())
                .toList();
    }
}