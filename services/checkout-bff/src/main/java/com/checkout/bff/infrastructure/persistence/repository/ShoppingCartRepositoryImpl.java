package com.checkout.bff.infrastructure.persistence.repository;

import com.checkout.bff.domain.entity.ShoppingCart;
import com.checkout.bff.domain.repository.ShoppingCartRepository;
import com.checkout.bff.infrastructure.persistence.jpa.ShoppingCartJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository 
@RequiredArgsConstructor
public class ShoppingCartRepositoryImpl implements ShoppingCartRepository {

    private final ShoppingCartJpaRepository shoppingCartJpaRepository;

    @Override
    public Optional<ShoppingCart> findById(Long id) {
        return shoppingCartJpaRepository.findById(id);
    }

    @Override
    public Optional<ShoppingCart> findByPublicId(UUID publicId) {
        return shoppingCartJpaRepository.findByPublicId(publicId);
    }

    @Override
    public Optional<ShoppingCart> findByPublicIdWithItems(UUID publicId) {
        return shoppingCartJpaRepository.findByPublicIdWithItems(publicId);
    }

    @Override
    public ShoppingCart save(ShoppingCart cart) {
        return shoppingCartJpaRepository.save(cart);
    }

    @Override
    public List<ShoppingCart> findExpiredPendingCarts(LocalDateTime expiryThreshold) {
        return shoppingCartJpaRepository.findExpiredPendingCarts(expiryThreshold);
    }
}