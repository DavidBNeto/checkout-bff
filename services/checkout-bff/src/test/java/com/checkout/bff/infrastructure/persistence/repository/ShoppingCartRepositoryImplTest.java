package com.checkout.bff.infrastructure.persistence.repository;

import com.checkout.bff.domain.entity.ShoppingCart;
import com.checkout.bff.infrastructure.persistence.jpa.ShoppingCartJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShoppingCartRepositoryImplTest {

    @Mock
    private ShoppingCartJpaRepository shoppingCartJpaRepository;

    private ShoppingCartRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new ShoppingCartRepositoryImpl(shoppingCartJpaRepository);
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("Should return cart when found by ID")
        void shouldReturnCartWhenFoundById() {
            // Given
            Long id = 1L;
            ShoppingCart cart = createShoppingCart();
            when(shoppingCartJpaRepository.findById(id)).thenReturn(Optional.of(cart));

            // When
            Optional<ShoppingCart> result = repository.findById(id);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(cart);
            verify(shoppingCartJpaRepository).findById(id);
        }

        @Test
        @DisplayName("Should return empty when not found by ID")
        void shouldReturnEmptyWhenNotFoundById() {
            // Given
            Long id = 1L;
            when(shoppingCartJpaRepository.findById(id)).thenReturn(Optional.empty());

            // When
            Optional<ShoppingCart> result = repository.findById(id);

            // Then
            assertThat(result).isEmpty();
            verify(shoppingCartJpaRepository).findById(id);
        }
    }

    @Nested
    @DisplayName("findByPublicId")
    class FindByPublicId {

        @Test
        @DisplayName("Should return cart when found by public ID")
        void shouldReturnCartWhenFoundByPublicId() {
            // Given
            UUID publicId = UUID.randomUUID();
            ShoppingCart cart = createShoppingCart();
            cart.setPublicId(publicId);
            when(shoppingCartJpaRepository.findByPublicId(publicId)).thenReturn(Optional.of(cart));

            // When
            Optional<ShoppingCart> result = repository.findByPublicId(publicId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getPublicId()).isEqualTo(publicId);
            verify(shoppingCartJpaRepository).findByPublicId(publicId);
        }

        @Test
        @DisplayName("Should return empty when not found by public ID")
        void shouldReturnEmptyWhenNotFoundByPublicId() {
            // Given
            UUID publicId = UUID.randomUUID();
            when(shoppingCartJpaRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

            // When
            Optional<ShoppingCart> result = repository.findByPublicId(publicId);

            // Then
            assertThat(result).isEmpty();
            verify(shoppingCartJpaRepository).findByPublicId(publicId);
        }
    }

    @Nested
    @DisplayName("findByPublicIdWithItems")
    class FindByPublicIdWithItems {

        @Test
        @DisplayName("Should return cart with items when found by public ID")
        void shouldReturnCartWithItemsWhenFoundByPublicId() {
            // Given
            UUID publicId = UUID.randomUUID();
            ShoppingCart cart = createShoppingCart();
            cart.setPublicId(publicId);
            when(shoppingCartJpaRepository.findByPublicIdWithItems(publicId)).thenReturn(Optional.of(cart));

            // When
            Optional<ShoppingCart> result = repository.findByPublicIdWithItems(publicId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getPublicId()).isEqualTo(publicId);
            verify(shoppingCartJpaRepository).findByPublicIdWithItems(publicId);
        }

        @Test
        @DisplayName("Should return empty when not found by public ID with items")
        void shouldReturnEmptyWhenNotFoundByPublicIdWithItems() {
            // Given
            UUID publicId = UUID.randomUUID();
            when(shoppingCartJpaRepository.findByPublicIdWithItems(publicId)).thenReturn(Optional.empty());

            // When
            Optional<ShoppingCart> result = repository.findByPublicIdWithItems(publicId);

            // Then
            assertThat(result).isEmpty();
            verify(shoppingCartJpaRepository).findByPublicIdWithItems(publicId);
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("Should save and return cart")
        void shouldSaveAndReturnCart() {
            // Given
            ShoppingCart cart = createShoppingCart();
            when(shoppingCartJpaRepository.save(cart)).thenReturn(cart);

            // When
            ShoppingCart result = repository.save(cart);

            // Then
            assertThat(result).isEqualTo(cart);
            verify(shoppingCartJpaRepository).save(cart);
        }
    }

    @Nested
    @DisplayName("findExpiredPendingCarts")
    class FindExpiredPendingCarts {

        @Test
        @DisplayName("Should return expired pending carts")
        void shouldReturnExpiredPendingCarts() {
            // Given
            LocalDateTime expiryThreshold = LocalDateTime.now().minusMinutes(30);
            ShoppingCart cart1 = createShoppingCart();
            ShoppingCart cart2 = createShoppingCart();
            cart2.setPublicId(UUID.randomUUID());
            when(shoppingCartJpaRepository.findExpiredPendingCarts(expiryThreshold)).thenReturn(List.of(cart1, cart2));

            // When
            List<ShoppingCart> result = repository.findExpiredPendingCarts(expiryThreshold);

            // Then
            assertThat(result).hasSize(2);
            verify(shoppingCartJpaRepository).findExpiredPendingCarts(expiryThreshold);
        }

        @Test
        @DisplayName("Should return empty list when no expired pending carts")
        void shouldReturnEmptyListWhenNoExpiredPendingCarts() {
            // Given
            LocalDateTime expiryThreshold = LocalDateTime.now().minusMinutes(30);
            when(shoppingCartJpaRepository.findExpiredPendingCarts(expiryThreshold)).thenReturn(List.of());

            // When
            List<ShoppingCart> result = repository.findExpiredPendingCarts(expiryThreshold);

            // Then
            assertThat(result).isEmpty();
            verify(shoppingCartJpaRepository).findExpiredPendingCarts(expiryThreshold);
        }
    }

    private ShoppingCart createShoppingCart() {
        ShoppingCart cart = ShoppingCart.builder()
                .price(com.checkout.bff.domain.valueobject.Money.of(java.math.BigDecimal.valueOf(100.00), "BRL"))
                .status("PENDING")
                .build();
        cart.setId(1L);
        cart.setPublicId(UUID.randomUUID());
        return cart;
    }
}