package com.checkout.bff.infrastructure.persistence.jpa;

import com.checkout.bff.domain.entity.CheckoutOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CheckoutOperationJpaRepository extends JpaRepository<CheckoutOperation, Long> {

    Optional<CheckoutOperation> findByPublicId(UUID publicId);

    Optional<CheckoutOperation> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT c FROM CheckoutOperation c WHERE c.cart.id = :cartId")
    List<CheckoutOperation> findByCartId(@Param("cartId") Long cartId);

    @Query("SELECT c FROM CheckoutOperation c WHERE c.status = 'PENDING' AND c.createdAt < :threshold")
    List<CheckoutOperation> findPendingOlderThan(@Param("threshold") LocalDateTime threshold);
}