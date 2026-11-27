package com.checkout.bff.application.port;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public interface CachePort {

    void set(String key, Object value, long timeout, TimeUnit unit);

    <T> Optional<T> get(String key, Class<T> type);

    <T> Optional<T> getTyped(String key, TypeReference<T> typeReference);

    void delete(String key);

    boolean exists(String key);
}