package com.checkout.bff.application.port;

import com.checkout.bff.domain.entity.PaymentMethod;

import java.util.List;
import java.util.Optional;

public interface PaymentMethodsServicePort {

    Optional<PaymentMethod> findByName(String name);

    Optional<PaymentMethod> findByNormalizedName(String normalizedName);

    List<PaymentMethod> findAllActive();
}