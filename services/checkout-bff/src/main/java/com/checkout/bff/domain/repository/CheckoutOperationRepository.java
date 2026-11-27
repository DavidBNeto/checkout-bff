package com.checkout.bff.domain.repository;

import com.checkout.bff.domain.entity.CheckoutOperation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckoutOperationRepository {

    Optional<CheckoutOperation> findById(Long id);

    Optional<CheckoutOperation> findByPublicId(UUID publicId);

    Optional<CheckoutOperation> findByIdempotencyKey(String idempotencyKey);

    List<CheckoutOperation> findByCartId(Long cartId);

    List<CheckoutOperation> findPendingOlderThan(LocalDateTime threshold);

    CheckoutOperation save(CheckoutOperation operation);
}