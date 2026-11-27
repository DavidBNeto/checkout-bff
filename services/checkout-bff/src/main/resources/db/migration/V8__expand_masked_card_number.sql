-- V8: Expand masked_card_number column to support formatted masked card numbers (e.g., "****1234")

ALTER TABLE checkout_operation
ALTER COLUMN masked_card_number TYPE VARCHAR(20);