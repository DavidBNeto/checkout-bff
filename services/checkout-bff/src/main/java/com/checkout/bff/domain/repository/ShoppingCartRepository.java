package com.checkout.bff.domain.repository;

import com.checkout.bff.domain.entity.ShoppingCart;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShoppingCartRepository {

    Optional<ShoppingCart> findById(Long id);

    Optional<ShoppingCart> findByPublicId(UUID publicId);

    Optional<ShoppingCart> findByPublicIdWithItems(UUID publicId);

    ShoppingCart save(ShoppingCart cart);

    List<ShoppingCart> findExpiredPendingCarts(LocalDateTime expiryThreshold);
}