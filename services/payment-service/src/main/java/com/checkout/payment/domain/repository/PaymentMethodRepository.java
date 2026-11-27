package com.checkout.payment.domain.repository;

import com.checkout.payment.domain.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    Optional<PaymentMethod> findByNormalizedName(String normalizedName);

    List<PaymentMethod> findByActiveTrue();
}