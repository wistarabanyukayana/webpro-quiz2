-- Created by Redgate Data Modeler (https://datamodeler.redgate-platform.com)
-- Last modification date: 2026-10-27 15:05:25.67

-- tables
-- Table: accounts
CREATE TABLE accounts (
    id integer NOT NULL CONSTRAINT accounts_pk PRIMARY KEY AUTOINCREMENT,
    username text NOT NULL CHECK (length(trim(username)) > 0),
    full_name text NOT NULL CHECK (length(trim(full_name)) > 0),
    email text NOT NULL,
    password_hash text NOT NULL,
    last_login text NOT NULL DEFAULT (datetime('now')),
    initial_balance integer NOT NULL DEFAULT 0,
    remember_token text,
    session_version integer NOT NULL DEFAULT 1,
    created_at text NOT NULL DEFAULT (datetime('now')),
    updated_at text NOT NULL DEFAULT (datetime('now'))
);

CREATE UNIQUE INDEX ux_akun_username
ON accounts (lower(username))
;

CREATE UNIQUE INDEX ux_akun_email
ON accounts (lower(email))
;

-- Table: categories
CREATE TABLE categories (
    id integer NOT NULL CONSTRAINT categories_pk PRIMARY KEY AUTOINCREMENT,
    name text NOT NULL CHECK (length(trim(name)) > 0),
    type text NOT NULL CHECK (type IN ('income', 'expense')),
    deleted_at text,
    accounts_id integer NOT NULL,
    CONSTRAINT categories_ak_1 UNIQUE (id, accounts_id),
    CONSTRAINT categories_accounts FOREIGN KEY (accounts_id)
    REFERENCES accounts (id)
    ON DELETE CASCADE
);

CREATE UNIQUE INDEX ux_kategori_aktif
ON categories (accounts_id ASC, name ASC, type ASC)
WHERE deleted_at IS NULL;

-- Table: default_categories
CREATE TABLE default_categories (
    id integer NOT NULL CONSTRAINT default_categories_pk PRIMARY KEY AUTOINCREMENT,
    name text NOT NULL,
    type text NOT NULL CHECK (type IN ('income', 'expense')),
    CONSTRAINT default_categories_ak_1 UNIQUE (name, type)
);

-- Table: transactions
CREATE TABLE transactions (
    id integer NOT NULL CONSTRAINT transactions_pk PRIMARY KEY AUTOINCREMENT,
    type text NOT NULL CHECK (type IN ('income', 'expense')),
    amount integer NOT NULL CHECK (amount > 0),
    notes text,
    date text NOT NULL CHECK (date(date) IS date),
    time text CHECK (time(time) IS time),
    created_at text NOT NULL DEFAULT (datetime('now')),
    updated_at text NOT NULL DEFAULT (datetime('now')),
    accounts_id integer NOT NULL,
    categories_id integer NOT NULL,
    CONSTRAINT transactions_accounts FOREIGN KEY (accounts_id)
    REFERENCES accounts (id)
    ON DELETE CASCADE,
    CONSTRAINT transactions_categories FOREIGN KEY (categories_id,accounts_id)
    REFERENCES categories (id,accounts_id)
);

CREATE INDEX ix_transaksi_akun_tanggal
ON transactions (accounts_id ASC, date ASC, time ASC)
;

CREATE INDEX ix_transactions_category
ON transactions (accounts_id, categories_id, date)
;

-- Table: budgets
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

CREATE TRIGGER trg_accounts_seed_categories AFTER INSERT ON accounts
BEGIN
    INSERT INTO categories (accounts_id, name, type)
    SELECT NEW.id, name, type FROM default_categories;
END;

CREATE TRIGGER trg_transactions_updated_at AFTER UPDATE ON transactions
WHEN NEW.updated_at = OLD.updated_at
BEGIN
    UPDATE transactions SET updated_at = datetime('now') WHERE id = NEW.id;
END;

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

CREATE TRIGGER trg_categories_delete_budgets AFTER UPDATE OF deleted_at ON categories
WHEN NEW.deleted_at IS NOT NULL AND OLD.deleted_at IS NULL
BEGIN
    DELETE FROM budgets WHERE categories_id = NEW.id;
END;

-- End of file.

