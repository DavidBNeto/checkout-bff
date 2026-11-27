-- Enable UUID extension if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- payment_method table
CREATE TABLE payment_method (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT true,
    normalized_name VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN' UNIQUE
);

-- product table
CREATE TABLE product (
    id BIGSERIAL PRIMARY KEY,
    slug VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    price NUMERIC(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true
);

-- payment_data table
CREATE TABLE payment_data (
    id BIGSERIAL PRIMARY KEY,
    method_id BIGINT NOT NULL REFERENCES payment_method(id),
    holder VARCHAR(255) NOT NULL,
    details TEXT NOT NULL
);

-- shopping_cart table
CREATE TABLE shopping_cart (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE DEFAULT uuid_generate_v4(),
    payment_data_id BIGINT NOT NULL UNIQUE REFERENCES payment_data(id),
    price NUMERIC(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(50) NOT NULL,
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- cart_item table
CREATE TABLE cart_item (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL REFERENCES shopping_cart(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES product(id),
    quantity INTEGER NOT NULL DEFAULT 1,
    UNIQUE(cart_id, product_id)
);

-- Indexes
CREATE INDEX idx_shopping_cart_payment_data_id ON shopping_cart(payment_data_id);
CREATE INDEX idx_shopping_cart_status ON shopping_cart(status);
CREATE INDEX idx_shopping_cart_created_at ON shopping_cart(created_at);
CREATE INDEX idx_cart_item_cart_id ON cart_item(cart_id);
CREATE INDEX idx_cart_item_product_id ON cart_item(product_id);
CREATE INDEX idx_product_slug ON product(slug);
CREATE INDEX idx_product_active ON product(active);
CREATE INDEX idx_payment_method_name ON payment_method(name);
CREATE INDEX idx_payment_method_active ON payment_method(active);
CREATE INDEX idx_payment_data_method_id ON payment_data(method_id);
CREATE INDEX idx_payment_method_normalized_name ON payment_method(normalized_name);