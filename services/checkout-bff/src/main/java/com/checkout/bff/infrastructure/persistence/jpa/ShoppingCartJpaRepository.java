package com.checkout.bff.infrastructure.persistence.jpa;

import com.checkout.bff.domain.entity.ShoppingCart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShoppingCartJpaRepository extends JpaRepository<ShoppingCart, Long> {

    @EntityGraph(attributePaths = {"items", "items.product", "availablePaymentMethods", "availablePaymentMethods.paymentMethod"})
    Optional<ShoppingCart> findById(Long id);

    @EntityGraph(attributePaths = {"items", "items.product", "availablePaymentMethods", "availablePaymentMethods.paymentMethod"})
    Optional<ShoppingCart> findByPublicId(UUID publicId);

    @Query("SELECT c FROM ShoppingCart c LEFT JOIN FETCH c.items i LEFT JOIN FETCH i.product LEFT JOIN FETCH c.availablePaymentMethods apm LEFT JOIN FETCH apm.paymentMethod WHERE c.publicId = :publicId")
    Optional<ShoppingCart> findByPublicIdWithItems(@Param("publicId") UUID publicId);

    @Query("SELECT c FROM ShoppingCart c WHERE c.status = 'PENDING' AND c.createdAt < :expiryThreshold")
    List<ShoppingCart> findExpiredPendingCarts(@Param("expiryThreshold") LocalDateTime expiryThreshold);
}