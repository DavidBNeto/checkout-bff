package com.checkout.bff.infrastructure.persistence.repository;

import com.checkout.bff.domain.entity.CheckoutOperation;
import com.checkout.bff.infrastructure.persistence.jpa.CheckoutOperationJpaRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckoutOperationRepositoryImplTest {

    @Mock
    private CheckoutOperationJpaRepository jpaRepository;

    private CheckoutOperationRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new CheckoutOperationRepositoryImpl(jpaRepository);
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("Should return operation when found by ID")
        void shouldReturnOperationWhenFoundById() {
            // Given
            Long id = 1L;
            CheckoutOperation operation = createCheckoutOperation();
            when(jpaRepository.findById(id)).thenReturn(Optional.of(operation));

            // When
            Optional<CheckoutOperation> result = repository.findById(id);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(operation);
            verify(jpaRepository).findById(id);
        }

        @Test
        @DisplayName("Should return empty when not found by ID")
        void shouldReturnEmptyWhenNotFoundById() {
            // Given
            Long id = 1L;
            when(jpaRepository.findById(id)).thenReturn(Optional.empty());

            // When
            Optional<CheckoutOperation> result = repository.findById(id);

            // Then
            assertThat(result).isEmpty();
            verify(jpaRepository).findById(id);
        }
    }

    @Nested
    @DisplayName("findByPublicId")
    class FindByPublicId {

        @Test
        @DisplayName("Should return operation when found by public ID")
        void shouldReturnOperationWhenFoundByPublicId() {
            // Given
            UUID publicId = UUID.randomUUID();
            CheckoutOperation operation = createCheckoutOperation();
            operation.setPublicId(publicId);
            when(jpaRepository.findByPublicId(publicId)).thenReturn(Optional.of(operation));

            // When
            Optional<CheckoutOperation> result = repository.findByPublicId(publicId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getPublicId()).isEqualTo(publicId);
            verify(jpaRepository).findByPublicId(publicId);
        }

        @Test
        @DisplayName("Should return empty when not found by public ID")
        void shouldReturnEmptyWhenNotFoundByPublicId() {
            // Given
            UUID publicId = UUID.randomUUID();
            when(jpaRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

            // When
            Optional<CheckoutOperation> result = repository.findByPublicId(publicId);

            // Then
            assertThat(result).isEmpty();
            verify(jpaRepository).findByPublicId(publicId);
        }
    }

    @Nested
    @DisplayName("findByIdempotencyKey")
    class FindByIdempotencyKey {

        @Test
        @DisplayName("Should return operation when found by idempotency key")
        void shouldReturnOperationWhenFoundByIdempotencyKey() {
            // Given
            String idempotencyKey = "idem-key-123";
            CheckoutOperation operation = createCheckoutOperation();
            operation.setIdempotencyKey(idempotencyKey);
            when(jpaRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(operation));

            // When
            Optional<CheckoutOperation> result = repository.findByIdempotencyKey(idempotencyKey);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getIdempotencyKey()).isEqualTo(idempotencyKey);
            verify(jpaRepository).findByIdempotencyKey(idempotencyKey);
        }

        @Test
        @DisplayName("Should return empty when not found by idempotency key")
        void shouldReturnEmptyWhenNotFoundByIdempotencyKey() {
            // Given
            String idempotencyKey = "idem-key-123";
            when(jpaRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());

            // When
            Optional<CheckoutOperation> result = repository.findByIdempotencyKey(idempotencyKey);

            // Then
            assertThat(result).isEmpty();
            verify(jpaRepository).findByIdempotencyKey(idempotencyKey);
        }
    }

    @Nested
    @DisplayName("findByCartId")
    class FindByCartId {

        @Test
        @DisplayName("Should return operations when found by cart ID")
        void shouldReturnOperationsWhenFoundByCartId() {
            // Given
            Long cartId = 1L;
            CheckoutOperation operation1 = createCheckoutOperation();
            CheckoutOperation operation2 = createCheckoutOperation();
            operation2.setPublicId(UUID.randomUUID());
            when(jpaRepository.findByCartId(cartId)).thenReturn(List.of(operation1, operation2));

            // When
            List<CheckoutOperation> result = repository.findByCartId(cartId);

            // Then
            assertThat(result).hasSize(2);
            verify(jpaRepository).findByCartId(cartId);
        }

        @Test
        @DisplayName("Should return empty list when no operations found by cart ID")
        void shouldReturnEmptyListWhenNoOperationsFoundByCartId() {
            // Given
            Long cartId = 1L;
            when(jpaRepository.findByCartId(cartId)).thenReturn(List.of());

            // When
            List<CheckoutOperation> result = repository.findByCartId(cartId);

            // Then
            assertThat(result).isEmpty();
            verify(jpaRepository).findByCartId(cartId);
        }
    }

    @Nested
    @DisplayName("findPendingOlderThan")
    class FindPendingOlderThan {

        @Test
        @DisplayName("Should return operations when found pending older than threshold")
        void shouldReturnOperationsWhenFoundPendingOlderThanThreshold() {
            // Given
            LocalDateTime threshold = LocalDateTime.now().minusHours(1);
            CheckoutOperation operation1 = createCheckoutOperation();
            CheckoutOperation operation2 = createCheckoutOperation();
            operation2.setPublicId(UUID.randomUUID());
            when(jpaRepository.findPendingOlderThan(threshold)).thenReturn(List.of(operation1, operation2));

            // When
            List<CheckoutOperation> result = repository.findPendingOlderThan(threshold);

            // Then
            assertThat(result).hasSize(2);
            verify(jpaRepository).findPendingOlderThan(threshold);
        }

        @Test
        @DisplayName("Should return empty list when no pending operations found")
        void shouldReturnEmptyListWhenNoPendingOperationsFound() {
            // Given
            LocalDateTime threshold = LocalDateTime.now().minusHours(1);
            when(jpaRepository.findPendingOlderThan(threshold)).thenReturn(List.of());

            // When
            List<CheckoutOperation> result = repository.findPendingOlderThan(threshold);

            // Then
            assertThat(result).isEmpty();
            verify(jpaRepository).findPendingOlderThan(threshold);
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("Should save and return operation")
        void shouldSaveAndReturnOperation() {
            // Given
            CheckoutOperation operation = createCheckoutOperation();
            when(jpaRepository.save(operation)).thenReturn(operation);

            // When
            CheckoutOperation result = repository.save(operation);

            // Then
            assertThat(result).isEqualTo(operation);
            verify(jpaRepository).save(operation);
        }
    }

    private CheckoutOperation createCheckoutOperation() {
        CheckoutOperation operation = CheckoutOperation.builder()
                .status("PENDING")
                .paymentMethodType("CREDIT_CARD")
                .paymentMethodName("Test Payment")
                .amount(java.math.BigDecimal.valueOf(100.00))
                .currency("BRL")
                .build();
        operation.setId(1L);
        operation.setPublicId(UUID.randomUUID());
        return operation;
    }
}