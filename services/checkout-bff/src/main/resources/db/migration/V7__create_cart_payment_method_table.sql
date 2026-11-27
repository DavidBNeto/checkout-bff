-- V7: Create cart_payment_method table for available payment methods at cart creation
-- This replaces the single payment_data reference with multiple payment method options

-- Create the new intermediary table
CREATE TABLE cart_payment_method (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL REFERENCES shopping_cart(id) ON DELETE CASCADE,
    public_cart_id UUID NOT NULL,
    payment_method_id BIGINT NOT NULL REFERENCES payment_method(id),
    payment_method_name VARCHAR(100) NOT NULL
);

-- Indexes for the new table
CREATE INDEX idx_cart_payment_method_cart_id ON cart_payment_method(cart_id);
CREATE INDEX idx_cart_payment_method_public_cart_id ON cart_payment_method(public_cart_id);
CREATE INDEX idx_cart_payment_method_payment_method_id ON cart_payment_method(payment_method_id);

-- Remove the old payment_data_id foreign key and column from shopping_cart
ALTER TABLE shopping_cart DROP CONSTRAINT IF EXISTS shopping_cart_payment_data_id_fkey;
ALTER TABLE shopping_cart DROP COLUMN IF EXISTS payment_data_id;

-- Drop the payment_data table as it's no longer needed
DROP TABLE IF EXISTS payment_data CASCADE;

-- Update indexes on shopping_cart
DROP INDEX IF EXISTS idx_shopping_cart_payment_data_id;