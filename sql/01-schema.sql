-- ---------------------------------------------------------------------------
-- Warehouse database schema (SQLite)
--
-- This file is the single source of truth for the schema. It is executed on
-- every application start via spring.sql.init (see application.yml) and is
-- written to be idempotent (CREATE ... IF NOT EXISTS).
--
-- Money is stored as integer minor units (for IDR the minor unit is the
-- rupiah, so a price of 75000 means Rp 75.000). Integers avoid floating point
-- rounding entirely.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS items (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    sku         TEXT      NOT NULL UNIQUE,
    name        TEXT      NOT NULL,
    description TEXT,
    base_price  INTEGER   NOT NULL CHECK (base_price >= 0),
    active      INTEGER   NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL
);

-- A variant is the actual stock-keeping unit (e.g. "Size M / Black").
-- price is an optional override; when NULL the item's base_price applies.
CREATE TABLE IF NOT EXISTS variants (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id    INTEGER   NOT NULL REFERENCES items (id),
    sku        TEXT      NOT NULL UNIQUE,
    name       TEXT      NOT NULL,
    price      INTEGER   CHECK (price >= 0),
    stock      INTEGER   NOT NULL DEFAULT 0 CHECK (stock >= 0),
    active     INTEGER   NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_variants_item_id ON variants (item_id);

CREATE TABLE IF NOT EXISTS sales (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    total_amount INTEGER   NOT NULL CHECK (total_amount >= 0),
    created_at   TIMESTAMP NOT NULL
);

-- One row per sold variant. unit_price is a snapshot so later price changes
-- never rewrite history.
CREATE TABLE IF NOT EXISTS sale_lines (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    sale_id    INTEGER NOT NULL REFERENCES sales (id),
    variant_id INTEGER NOT NULL REFERENCES variants (id),
    quantity   INTEGER NOT NULL CHECK (quantity > 0),
    unit_price INTEGER NOT NULL CHECK (unit_price >= 0),
    line_total INTEGER NOT NULL CHECK (line_total >= 0)
);

CREATE INDEX IF NOT EXISTS idx_sale_lines_sale_id ON sale_lines (sale_id);
CREATE INDEX IF NOT EXISTS idx_sale_lines_variant_id ON sale_lines (variant_id);

-- Append-only audit trail: every stock change is one row, written in the same
-- transaction as the stock update, so the ledger always explains the stock.
--   INITIAL    - starting stock when a variant is created
--   SALE       - stock leaving because of a sale (reference_id = sales.id)
--   ADJUSTMENT - manual correction / restock
CREATE TABLE IF NOT EXISTS stock_movements (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    variant_id     INTEGER   NOT NULL REFERENCES variants (id),
    type           TEXT      NOT NULL CHECK (type IN ('INITIAL', 'SALE', 'ADJUSTMENT')),
    quantity_delta INTEGER   NOT NULL CHECK (quantity_delta <> 0),
    stock_after    INTEGER   NOT NULL CHECK (stock_after >= 0),
    reference_id   INTEGER,
    reason         TEXT,
    created_at     TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_stock_movements_variant_id
    ON stock_movements (variant_id, id);

-- Idempotency records for unsafe endpoints (currently POST /api/v1/sales).
-- A row is inserted at the very start of the business transaction, so the key
-- and the side effects commit atomically: a failed request leaves no key
-- behind and can safely be retried.
CREATE TABLE IF NOT EXISTS idempotency_keys (
    key                 TEXT PRIMARY KEY,
    request_fingerprint TEXT      NOT NULL,
    response_status     INTEGER,
    response_body       TEXT,
    resource_id         INTEGER,
    created_at          TIMESTAMP NOT NULL
);
