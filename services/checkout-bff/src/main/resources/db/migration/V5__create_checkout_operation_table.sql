-- V5: Create checkout_operation table for async checkout tracking

CREATE TABLE checkout_operation (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    cart_id BIGINT NOT NULL REFERENCES shopping_cart(id),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    payment_method_type VARCHAR(50) NOT NULL,
    payment_method_name VARCHAR(100),
    amount NUMERIC(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    masked_card_number VARCHAR(4),
    pix_code TEXT,
    authorization_code VARCHAR(100),
    failure_reason VARCHAR(500),
    idempotency_key VARCHAR(100) UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    processed_at TIMESTAMP
);

-- Indexes
CREATE INDEX idx_checkout_operation_cart_id ON checkout_operation(cart_id);
CREATE INDEX idx_checkout_operation_public_id ON checkout_operation(public_id);
CREATE INDEX idx_checkout_operation_status ON checkout_operation(status);
CREATE INDEX idx_checkout_operation_idempotency_key ON checkout_operation(idempotency_key);
CREATE INDEX idx_checkout_operation_created_at ON checkout_operation(created_at);

-- Status check constraint
ALTER TABLE checkout_operation
ADD CONSTRAINT chk_checkout_operation_status
CHECK (status IN ('PENDING', 'PROCESSING', 'APPROVED', 'DECLINED', 'FAILED'));