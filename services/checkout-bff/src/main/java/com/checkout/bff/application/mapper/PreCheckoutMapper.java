package com.checkout.bff.application.mapper;

import com.checkout.bff.application.dto.PreCheckoutResponse;
import com.checkout.bff.domain.entity.PaymentMethod;
import com.checkout.bff.domain.entity.Product;
import com.checkout.bff.domain.entity.ShoppingCart;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PreCheckoutMapper {

    public PreCheckoutResponse toResponse(ShoppingCart cart,
                                          List<Product> products,
                                          List<PaymentMethod> paymentMethods) {
        return PreCheckoutResponse.builder()
                .cartId(cart.getPublicId())
                .products(mapProducts(products))
                .paymentMethods(mapPaymentMethods(paymentMethods))
                .totalAmount(cart.getPrice().getAmount())
                .currency(cart.getPrice().getCurrency().getCurrencyCode())
                .build();
    }

    private List<PreCheckoutResponse.ProductDetail> mapProducts(List<Product> products) {
        return products.stream()
                .map(p -> PreCheckoutResponse.ProductDetail.builder()
                        .slug(p.getSlug())
                        .name(p.getName())
                        .description(p.getDescription())
                        .price(p.getPrice().getAmount())
                        .currency(p.getPrice().getCurrency().getCurrencyCode())
                        .build())
                .toList();
    }

    private List<PreCheckoutResponse.PaymentMethodDetail> mapPaymentMethods(List<PaymentMethod> paymentMethods) {
        return paymentMethods.stream()
                .filter(PaymentMethod::isActive)
                .map(pm -> PreCheckoutResponse.PaymentMethodDetail.builder()
                        .name(pm.getName())
                        .build())
                .toList();
    }
}