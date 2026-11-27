package com.checkout.bff.infrastructure.persistence.jpa;

import com.checkout.bff.domain.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodJpaRepository extends JpaRepository<PaymentMethod, Long> {

    Optional<PaymentMethod> findByName(String name);

    List<PaymentMethod> findByActiveTrue();
}