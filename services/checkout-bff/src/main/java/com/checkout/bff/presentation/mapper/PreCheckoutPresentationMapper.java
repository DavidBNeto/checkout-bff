package com.checkout.bff.presentation.mapper;

import com.checkout.bff.application.dto.PreCheckoutResponse;
import com.checkout.bff.presentation.dto.PreCheckoutPresentationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PreCheckoutPresentationMapper {

    public PreCheckoutPresentationResponse toResponse(PreCheckoutResponse applicationResponse) {
        return PreCheckoutPresentationResponse.builder()
                .cartId(applicationResponse.getCartId())
                .products(mapProducts(applicationResponse.getProducts()))
                .paymentMethods(mapPaymentMethods(applicationResponse.getPaymentMethods()))
                .totalAmount(applicationResponse.getTotalAmount())
                .currency(applicationResponse.getCurrency())
                .build();
    }

    private List<PreCheckoutPresentationResponse.ProductDetail> mapProducts(List<PreCheckoutResponse.ProductDetail> products) {
        return products.stream()
                .map(p -> PreCheckoutPresentationResponse.ProductDetail.builder()
                        .slug(p.getSlug())
                        .name(p.getName())
                        .description(p.getDescription())
                        .price(p.getPrice())
                        .currency(p.getCurrency())
                        .build())
                .toList();
    }

    private List<PreCheckoutPresentationResponse.PaymentMethodDetail> mapPaymentMethods(List<PreCheckoutResponse.PaymentMethodDetail> paymentMethods) {
        return paymentMethods.stream()
                .map(pm -> PreCheckoutPresentationResponse.PaymentMethodDetail.builder()
                        .name(pm.getName())
                        .build())
                .toList();
    }
}