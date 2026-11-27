package com.checkout.product.config;

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

        if ("findBySlug".equals(methodName) && params.length == 1) {
            return "product:slug:" + params[0];
        }

        if ("findBySlugInAndActiveTrue".equals(methodName) && params.length == 1) {
            return "products:active";
        }

        if ("findAllActive".equals(methodName)) {
            return "products:active";
        }

        // Fallback to default Spring cache key generation
        return fallbackGenerator.generate(target, method, params);
    }
}