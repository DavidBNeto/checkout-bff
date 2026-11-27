package com.checkout.payment.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessedEventStore {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String KEY_PREFIX = "payment:processed:";
    private static final Duration TTL = Duration.ofDays(7);

    public boolean isProcessed(String checkoutId) {
        String key = KEY_PREFIX + checkoutId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void markProcessed(String checkoutId, String eventId) {
        String key = KEY_PREFIX + checkoutId;
        redisTemplate.opsForValue().set(key, eventId, TTL);
        log.debug("Marked checkout {} as processed with event {}", checkoutId, eventId);
    }
}