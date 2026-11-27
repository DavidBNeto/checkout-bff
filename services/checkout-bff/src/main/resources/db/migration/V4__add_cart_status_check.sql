ALTER TABLE shopping_cart
ADD CONSTRAINT chk_shopping_cart_status
CHECK (status IN ('PENDING', 'PAID', 'EXPIRED', 'CANCELLED'));