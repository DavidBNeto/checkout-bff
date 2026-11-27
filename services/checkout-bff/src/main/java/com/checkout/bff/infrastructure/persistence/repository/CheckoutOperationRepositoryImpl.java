package com.checkout.bff.infrastructure.persistence.repository;

import com.checkout.bff.domain.entity.CheckoutOperation;
import com.checkout.bff.domain.repository.CheckoutOperationRepository;
import com.checkout.bff.infrastructure.persistence.jpa.CheckoutOperationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CheckoutOperationRepositoryImpl implements CheckoutOperationRepository {

    private final CheckoutOperationJpaRepository jpaRepository;

    @Override
    public Optional<CheckoutOperation> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<CheckoutOperation> findByPublicId(UUID publicId) {
        return jpaRepository.findByPublicId(publicId);
    }

    @Override
    public Optional<CheckoutOperation> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    public List<CheckoutOperation> findByCartId(Long cartId) {
        return jpaRepository.findByCartId(cartId);
    }

    @Override
    public List<CheckoutOperation> findPendingOlderThan(LocalDateTime threshold) {
        return jpaRepository.findPendingOlderThan(threshold);
    }

    @Override
    public CheckoutOperation save(CheckoutOperation operation) {
        return jpaRepository.save(operation);
    }
}