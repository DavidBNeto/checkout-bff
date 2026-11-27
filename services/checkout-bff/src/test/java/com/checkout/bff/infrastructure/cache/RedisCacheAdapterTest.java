package com.checkout.bff.infrastructure.cache;

import com.checkout.bff.application.port.CachePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisCacheAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    private RedisCacheAdapter redisCacheAdapter;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        redisCacheAdapter = new RedisCacheAdapter(redisTemplate, objectMapper);
    }

    @Nested
    @DisplayName("Set")
    class Set {

        @Test
        @DisplayName("Should set value in cache successfully")
        void shouldSetValueInCacheSuccessfully() {
            // Given
            String key = "test-key";
            String value = "test-value";
            long timeout = 10;
            TimeUnit unit = TimeUnit.MINUTES;

            // When
            redisCacheAdapter.set(key, value, timeout, unit);

            // Then
            verify(valueOperations).set(eq(key), eq(value), eq(timeout), eq(unit));
        }

        @Test
        @DisplayName("Should handle exception gracefully when setting cache")
        void shouldHandleExceptionGracefullyWhenSettingCache() {
            // Given
            String key = "test-key";
            String value = "test-value";
            long timeout = 10;
            TimeUnit unit = TimeUnit.MINUTES;
            doThrow(new RuntimeException("Redis connection failed")).when(valueOperations).set(anyString(), any(), anyLong(), any());

            // When
            redisCacheAdapter.set(key, value, timeout, unit);

            // Then - should not throw exception
            verify(valueOperations).set(eq(key), eq(value), eq(timeout), eq(unit));
        }
    }

    @Nested
    @DisplayName("Get")
    class Get {

        @Test
        @DisplayName("Should return value when cache hit")
        void shouldReturnValueWhenCacheHit() {
            // Given
            String key = "test-key";
            String expectedValue = "test-value";
            when(valueOperations.get(key)).thenReturn(expectedValue);

            // When
            Optional<String> result = redisCacheAdapter.get(key, String.class);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(expectedValue);
            verify(valueOperations).get(key);
        }

        @Test
        @DisplayName("Should return empty when cache miss")
        void shouldReturnEmptyWhenCacheMiss() {
            // Given
            String key = "test-key";
            when(valueOperations.get(key)).thenReturn(null);

            // When
            Optional<String> result = redisCacheAdapter.get(key, String.class);

            // Then
            assertThat(result).isEmpty();
            verify(valueOperations).get(key);
        }

        @Test
        @DisplayName("Should return empty when type mismatch")
        void shouldReturnEmptyWhenTypeMismatch() {
            // Given
            String key = "test-key";
            Integer storedValue = 123;
            when(valueOperations.get(key)).thenReturn(storedValue);

            // When
            Optional<String> result = redisCacheAdapter.get(key, String.class);

            // Then
            assertThat(result).isEmpty();
            verify(valueOperations).get(key);
        }

        @Test
        @DisplayName("Should handle exception gracefully when getting cache")
        void shouldHandleExceptionGracefullyWhenGettingCache() {
            // Given
            String key = "test-key";
            when(valueOperations.get(key)).thenThrow(new RuntimeException("Redis connection failed"));

            // When
            Optional<String> result = redisCacheAdapter.get(key, String.class);

            // Then
            assertThat(result).isEmpty();
            verify(valueOperations).get(key);
        }
    }

    @Nested
    @DisplayName("Delete")
    class Delete {

        @Test
        @DisplayName("Should delete key from cache successfully")
        void shouldDeleteKeyFromCacheSuccessfully() {
            // Given
            String key = "test-key";

            // When
            redisCacheAdapter.delete(key);

            // Then
            verify(redisTemplate).delete(key);
        }

        @Test
        @DisplayName("Should handle exception gracefully when deleting cache")
        void shouldHandleExceptionGracefullyWhenDeletingCache() {
            // Given
            String key = "test-key";
            doThrow(new RuntimeException("Redis connection failed")).when(redisTemplate).delete(key);

            // When
            redisCacheAdapter.delete(key);

            // Then - should not throw exception
            verify(redisTemplate).delete(key);
        }
    }

    @Nested
    @DisplayName("Exists")
    class Exists {

        @Test
        @DisplayName("Should return true when key exists")
        void shouldReturnTrueWhenKeyExists() {
            // Given
            String key = "test-key";
            when(redisTemplate.hasKey(key)).thenReturn(true);

            // When
            boolean result = redisCacheAdapter.exists(key);

            // Then
            assertThat(result).isTrue();
            verify(redisTemplate).hasKey(key);
        }

        @Test
        @DisplayName("Should return false when key does not exist")
        void shouldReturnFalseWhenKeyDoesNotExist() {
            // Given
            String key = "test-key";
            when(redisTemplate.hasKey(key)).thenReturn(false);

            // When
            boolean result = redisCacheAdapter.exists(key);

            // Then
            assertThat(result).isFalse();
            verify(redisTemplate).hasKey(key);
        }

        @Test
        @DisplayName("Should return false when exception occurs")
        void shouldReturnFalseWhenExceptionOccurs() {
            // Given
            String key = "test-key";
            when(redisTemplate.hasKey(key)).thenThrow(new RuntimeException("Redis connection failed"));

            // When
            boolean result = redisCacheAdapter.exists(key);

            // Then
            assertThat(result).isFalse();
            verify(redisTemplate).hasKey(key);
        }
    }
}