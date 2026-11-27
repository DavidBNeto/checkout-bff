package com.checkout.bff.infrastructure.cache;

import com.checkout.bff.application.port.CachePort;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCacheAdapter implements CachePort {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            log.debug("Cache set: key={}, timeout={} {}", key, timeout, unit);
        } catch (Exception e) {
            log.warn("Failed to set cache key {}: {}", key, e.getMessage());
        }
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null && type.isInstance(value)) {
                log.debug("Cache hit: key={}", key);
                return Optional.of(type.cast(value));
            }
            log.debug("Cache miss: key={}", key);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Failed to get cache key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public <T> Optional<T> getTyped(String key, TypeReference<T> typeReference) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.debug("Cache hit: key={}", key);
                // Deserialize using ObjectMapper with full generic type information
                @SuppressWarnings("unchecked")
                T result = objectMapper.convertValue(value, typeReference);
                return Optional.of(result);
            }
            log.debug("Cache miss: key={}", key);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Failed to get cache key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Cache deleted: key={}", key);
        } catch (Exception e) {
            log.warn("Failed to delete cache key {}: {}", key, e.getMessage());
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("Failed to check cache key {}: {}", key, e.getMessage());
            return false;
        }
    }
}