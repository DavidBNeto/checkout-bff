package com.checkout.bff.infrastructure.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Duration;

@ExtendWith(MockitoExtension.class)
class ProcessedEventStoreImplTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ProcessedEventStoreImpl processedEventStore;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        processedEventStore = new ProcessedEventStoreImpl(redisTemplate);
    }

    @Nested
    @DisplayName("isProcessed")
    class IsProcessed {

        @Test
        @DisplayName("Should return true when event is processed")
        void shouldReturnTrueWhenEventIsProcessed() {
            // Given
            String eventId = "event-123";
            when(redisTemplate.hasKey("processed:event:" + eventId)).thenReturn(true);

            // When
            boolean result = processedEventStore.isProcessed(eventId);

            // Then
            assertThat(result).isTrue();
            verify(redisTemplate).hasKey("processed:event:" + eventId);
        }

        @Test
        @DisplayName("Should return false when event is not processed")
        void shouldReturnFalseWhenEventIsNotProcessed() {
            // Given
            String eventId = "event-123";
            when(redisTemplate.hasKey("processed:event:" + eventId)).thenReturn(false);

            // When
            boolean result = processedEventStore.isProcessed(eventId);

            // Then
            assertThat(result).isFalse();
            verify(redisTemplate).hasKey("processed:event:" + eventId);
        }

        @Test
        @DisplayName("Should return false when null returned")
        void shouldReturnFalseWhenNullReturned() {
            // Given
            String eventId = "event-123";
            when(redisTemplate.hasKey("processed:event:" + eventId)).thenReturn(null);

            // When
            boolean result = processedEventStore.isProcessed(eventId);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("markProcessed")
    class MarkProcessed {

        @Test
        @DisplayName("Should mark event as processed with checkout ID and TTL")
        void shouldMarkEventAsProcessedWithCheckoutIdAndTtl() {
            // Given
            String eventId = "event-123";
            String checkoutId = "checkout-456";

            // When
            processedEventStore.markProcessed(eventId, checkoutId);

            // Then
            verify(valueOperations).set(eq("processed:event:" + eventId), eq(checkoutId), any(Duration.class));
        }
    }
}