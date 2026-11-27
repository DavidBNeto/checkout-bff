package com.checkout.bff.presentation.mapper;

import com.checkout.bff.application.dto.SyncCheckoutResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SyncCheckoutPresentationMapper {

    public com.checkout.bff.presentation.dto.SyncCheckoutResponse toResponse(
            SyncCheckoutResponse applicationResponse) {
        return com.checkout.bff.presentation.dto.SyncCheckoutResponse.builder()
                .cartId(applicationResponse.getCartId())
                .status(applicationResponse.getStatus())
                .paidAt(applicationResponse.getPaidAt())
                .payment(mapPaymentDetails(applicationResponse.getPayment()))
                .products(mapProducts(applicationResponse.getProducts()))
                .build();
    }

    private com.checkout.bff.presentation.dto.SyncCheckoutResponse.PaymentDetails mapPaymentDetails(
            SyncCheckoutResponse.PaymentDetails payment) {
        return com.checkout.bff.presentation.dto.SyncCheckoutResponse.PaymentDetails.builder()
                .method(payment.getMethod())
                .lastDigits(payment.getLastDigits())
                .pixCode(payment.getPixCode())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .build();
    }

    private List<com.checkout.bff.presentation.dto.SyncCheckoutResponse.ProductDetail> mapProducts(
            List<SyncCheckoutResponse.ProductDetail> products) {
        return products.stream()
                .map(p -> com.checkout.bff.presentation.dto.SyncCheckoutResponse.ProductDetail.builder()
                        .slug(p.getSlug())
                        .name(p.getName())
                        .price(p.getPrice())
                        .build())
                .toList();
    }
}