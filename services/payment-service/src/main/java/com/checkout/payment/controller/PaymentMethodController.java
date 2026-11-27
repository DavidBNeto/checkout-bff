package com.checkout.payment.controller;

import com.checkout.payment.domain.entity.PaymentMethod;
import com.checkout.payment.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @GetMapping
    public ResponseEntity<List<PaymentMethod>> getPaymentMethods(
            @RequestParam(required = false, defaultValue = "true") boolean active) {

        log.debug("GET /api/v1/payment-methods - active: {}", active);

        if (active) {
            List<PaymentMethod> paymentMethods = paymentMethodService.findAllActive();
            return ResponseEntity.ok(paymentMethods);
        }

        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/{name}")
    public ResponseEntity<PaymentMethod> getPaymentMethodByName(@PathVariable String name) {
        log.debug("GET /api/v1/payment-methods/{}", name);
        return paymentMethodService.findByNormalizedName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}