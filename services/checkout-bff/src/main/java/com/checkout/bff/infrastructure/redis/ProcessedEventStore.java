package com.checkout.bff.infrastructure.redis;

public interface ProcessedEventStore {

    boolean isProcessed(String eventId);

    void markProcessed(String eventId, String checkoutId);
}