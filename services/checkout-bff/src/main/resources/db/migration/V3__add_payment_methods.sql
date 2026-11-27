INSERT INTO payment_method (name, normalized_name, active) VALUES
('Cartão de Crédito', 'CREDIT_CARD', true),
('Cartão de Débito', 'DEBIT_CARD', true),
('Pix', 'PIX', true)
ON CONFLICT (name) DO NOTHING;