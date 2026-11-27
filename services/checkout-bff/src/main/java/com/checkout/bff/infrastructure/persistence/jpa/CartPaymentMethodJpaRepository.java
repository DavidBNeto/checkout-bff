package com.checkout.bff.infrastructure.persistence.jpa;

import com.checkout.bff.domain.entity.CartPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartPaymentMethodJpaRepository extends JpaRepository<CartPaymentMethod, Long> {

    List<CartPaymentMethod> findByCartId(Long cartId);

    List<CartPaymentMethod> findByPublicCartId(UUID publicCartId);

    @Query("SELECT cpm FROM CartPaymentMethod cpm LEFT JOIN FETCH cpm.paymentMethod WHERE cpm.publicCartId = :publicCartId")
    List<CartPaymentMethod> findByPublicCartIdWithPaymentMethod(@Param("publicCartId") UUID publicCartId);

    Optional<CartPaymentMethod> findByPublicCartIdAndPaymentMethodName(@Param("publicCartId") UUID publicCartId, @Param("name") String name);
}