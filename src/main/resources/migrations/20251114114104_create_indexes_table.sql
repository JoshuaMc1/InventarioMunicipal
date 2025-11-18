CREATE INDEX IF NOT EXISTS idx_people_dni ON people (dni);
CREATE INDEX IF NOT EXISTS idx_products_name ON products (name);
CREATE INDEX IF NOT EXISTS idx_transactions_type ON transactions (type);
CREATE INDEX IF NOT EXISTS idx_audit_user_id ON audit (user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_product_id ON transactions (product_id);
CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON transactions (user_id);

