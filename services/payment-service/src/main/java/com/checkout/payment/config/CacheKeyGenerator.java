package com.checkout.payment.config;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
public class CacheKeyGenerator implements KeyGenerator {

    private final SimpleKeyGenerator fallbackGenerator = new SimpleKeyGenerator();

    @Override
    @NonNull
    public Object generate(@NonNull Object target, @NonNull Method method, @NonNull Object... params) {
        String methodName = method.getName();

        if ("findByName".equals(methodName) && params.length == 1) {
            return "payment-method:name:" + params[0];
        }

        if ("findAllActive".equals(methodName)) {
            return "payment-methods:active";
        }

        // Fallback to default Spring cache key generation
        return fallbackGenerator.generate(target, method, params);
    }
}