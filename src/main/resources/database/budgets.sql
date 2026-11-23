-- monthly budget per expense category

CREATE TABLE budgets (
    id integer NOT NULL CONSTRAINT budgets_pk PRIMARY KEY AUTOINCREMENT,
    month text NOT NULL CHECK (month GLOB '[0-9][0-9][0-9][0-9]-[01][0-9]' AND substr(month, 6) BETWEEN '01' AND '12'),
    amount integer NOT NULL CHECK (amount > 0),
    created_at text NOT NULL DEFAULT (datetime('now')),
    updated_at text NOT NULL DEFAULT (datetime('now')),
    accounts_id integer NOT NULL,
    categories_id integer NOT NULL,
    CONSTRAINT budgets_ak_1 UNIQUE (accounts_id, categories_id, month),
    CONSTRAINT budgets_accounts FOREIGN KEY (accounts_id)
    REFERENCES accounts (id)
    ON DELETE CASCADE,
    CONSTRAINT budgets_categories FOREIGN KEY (categories_id, accounts_id)
    REFERENCES categories (id, accounts_id)
    ON DELETE CASCADE
);

-- expense categories only
CREATE TRIGGER trg_budgets_expense_insert BEFORE INSERT ON budgets
WHEN (SELECT type FROM categories WHERE id = NEW.categories_id) IS NOT 'expense'
BEGIN
    SELECT RAISE(ABORT, 'budget category must be an expense category');
END;

CREATE TRIGGER trg_budgets_expense_update BEFORE UPDATE OF categories_id ON budgets
WHEN (SELECT type FROM categories WHERE id = NEW.categories_id) IS NOT 'expense'
BEGIN
    SELECT RAISE(ABORT, 'budget category must be an expense category');
END;

CREATE TRIGGER trg_categories_type_budgets BEFORE UPDATE OF type ON categories
WHEN NEW.type IS NOT OLD.type
    AND EXISTS (SELECT 1 FROM budgets WHERE categories_id = NEW.id)
BEGIN
    SELECT RAISE(ABORT, 'category type is locked while it has budgets');
END;

-- categories are soft deleted, so delete their budgets here
CREATE TRIGGER trg_categories_delete_budgets AFTER UPDATE OF deleted_at ON categories
WHEN NEW.deleted_at IS NOT NULL AND OLD.deleted_at IS NULL
BEGIN
    DELETE FROM budgets WHERE categories_id = NEW.id;
END;
