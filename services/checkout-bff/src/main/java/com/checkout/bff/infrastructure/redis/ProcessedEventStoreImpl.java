package com.checkout.bff.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class ProcessedEventStoreImpl implements ProcessedEventStore {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String PREFIX = "processed:event:";
    private static final Duration TTL = Duration.ofDays(7);

    @Override
    public boolean isProcessed(String eventId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + eventId));
    }

    @Override
    public void markProcessed(String eventId, String checkoutId) {
        redisTemplate.opsForValue().set(PREFIX + eventId, checkoutId, TTL);
    }
}