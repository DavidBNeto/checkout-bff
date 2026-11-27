package com.checkout.payment.service;

import com.checkout.payment.domain.entity.PaymentMethod;
import com.checkout.payment.domain.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;

    public Optional<PaymentMethod> findByNormalizedName(String normalizedName) {
        log.debug("Fetching payment method by normalized name from database: {}", normalizedName);
        return paymentMethodRepository.findByNormalizedName(normalizedName);
    }

    @Cacheable(value = "payment-methods", keyGenerator = "paymentCacheKeyGenerator")
    public List<PaymentMethod> findAllActive() {
        log.debug("Fetching all active payment methods from database");
        return paymentMethodRepository.findByActiveTrue();
    }
}