INSERT INTO product (slug, name, description, price, currency, active) VALUES
('basic', 'Basic', 'Basic subscription plan', 19.90, 'BRL', true),
('premium', 'Premium', 'Premium subscription plan', 39.90, 'BRL', true),
('family', 'Family', 'Family subscription plan', 59.90, 'BRL', true)
ON CONFLICT (slug) DO NOTHING;