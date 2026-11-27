-- V1: Create payment_method table
-- This is the base table for payment methods in the payment service

CREATE TABLE payment_method (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    normalized_name VARCHAR(50) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT true
);

CREATE INDEX idx_payment_method_name ON payment_method(name);
CREATE INDEX idx_payment_method_normalized_name ON payment_method(normalized_name);
CREATE INDEX idx_payment_method_active ON payment_method(active);