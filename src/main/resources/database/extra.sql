-- extra stuff on top of the PDM export (schema.sql)

-- sha-256 of the remember me token
ALTER TABLE accounts ADD COLUMN remember_token text;

-- bumped on password change to log out other sessions
ALTER TABLE accounts ADD COLUMN session_version integer NOT NULL DEFAULT 1;

-- transaction type must match the category type
CREATE TRIGGER trg_transactions_type_insert BEFORE INSERT ON transactions
WHEN NEW.type IS NOT (SELECT type FROM categories WHERE id = NEW.categories_id)
BEGIN
    SELECT RAISE(ABORT, 'transaction type must match category type');
END;

CREATE TRIGGER trg_transactions_type_update BEFORE UPDATE OF type, categories_id ON transactions
WHEN NEW.type IS NOT (SELECT type FROM categories WHERE id = NEW.categories_id)
BEGIN
    SELECT RAISE(ABORT, 'transaction type must match category type');
END;

CREATE TRIGGER trg_categories_type_locked BEFORE UPDATE OF type ON categories
WHEN NEW.type IS NOT OLD.type
    AND EXISTS (SELECT 1 FROM transactions WHERE categories_id = NEW.id)
BEGIN
    SELECT RAISE(ABORT, 'category type is locked while it has transactions');
END;
