-- ---------------------------------------------------------------------------
-- Seed data (SQLite)
--
-- Executed on every application start after 01-schema.sql. Every statement is
-- idempotent (INSERT OR IGNORE with fixed ids), so restarting the app never
-- duplicates rows and never overwrites changes made through the API.
--
-- Timestamps are stored as epoch milliseconds (INTEGER), which is how the
-- SQLite JDBC driver persists java.sql.Timestamp / java.time.Instant values.
-- ---------------------------------------------------------------------------

INSERT OR IGNORE INTO items (id, sku, name, description, base_price, active, created_at, updated_at) VALUES
    (1, 'TSHIRT-BASIC', 'Basic Cotton T-Shirt', 'Unisex 30s combed cotton tee', 75000, 1,
     CAST(strftime('%s', 'now') AS INTEGER) * 1000, CAST(strftime('%s', 'now') AS INTEGER) * 1000),
    (2, 'SNEAKER-RUN', 'Running Shoes', 'Lightweight daily running shoes', 450000, 1,
     CAST(strftime('%s', 'now') AS INTEGER) * 1000, CAST(strftime('%s', 'now') AS INTEGER) * 1000),
    (3, 'CAP-DENIM', 'Denim Cap', 'Adjustable denim baseball cap', 120000, 1,
     CAST(strftime('%s', 'now') AS INTEGER) * 1000, CAST(strftime('%s', 'now') AS INTEGER) * 1000),
    (4, 'MUG-CERAMIC', 'Ceramic Mug', 'Coming soon - no variants yet', 60000, 1,
     CAST(strftime('%s', 'now') AS INTEGER) * 1000, CAST(strftime('%s', 'now') AS INTEGER) * 1000);

-- price NULL means "use the item's base_price"; a value is an override.
INSERT OR IGNORE INTO variants (id, item_id, sku, name, price, stock, active, created_at, updated_at) VALUES
    (1, 1, 'TSHIRT-BASIC-M', 'M / Black', NULL, 25, 1,
     CAST(strftime('%s', 'now') AS INTEGER) * 1000, CAST(strftime('%s', 'now') AS INTEGER) * 1000),
    (2, 1, 'TSHIRT-BASIC-L', 'L / Navy',  NULL, 10, 1,
     CAST(strftime('%s', 'now') AS INTEGER) * 1000, CAST(strftime('%s', 'now') AS INTEGER) * 1000),
    (3, 2, 'SNEAKER-RUN-40', 'Size 40', NULL, 5, 1,
     CAST(strftime('%s', 'now') AS INTEGER) * 1000, CAST(strftime('%s', 'now') AS INTEGER) * 1000),
    (4, 2, 'SNEAKER-RUN-41', 'Size 41', NULL, 3, 1,
     CAST(strftime('%s', 'now') AS INTEGER) * 1000, CAST(strftime('%s', 'now') AS INTEGER) * 1000),
    (5, 2, 'SNEAKER-RUN-42', 'Size 42', NULL, 0, 1,
     CAST(strftime('%s', 'now') AS INTEGER) * 1000, CAST(strftime('%s', 'now') AS INTEGER) * 1000),
    (6, 3, 'CAP-DENIM-NVY', 'Navy', 99000, 15, 1,
     CAST(strftime('%s', 'now') AS INTEGER) * 1000, CAST(strftime('%s', 'now') AS INTEGER) * 1000),
    (7, 3, 'CAP-DENIM-BLK', 'Black', 109000, 8, 1,
     CAST(strftime('%s', 'now') AS INTEGER) * 1000, CAST(strftime('%s', 'now') AS INTEGER) * 1000);

-- The ledger starts with one INITIAL movement per seeded variant so that
-- stock and its audit trail are consistent from the very first start.
INSERT OR IGNORE INTO stock_movements (id, variant_id, type, quantity_delta, stock_after, reference_id, reason, created_at) VALUES
    (1, 1, 'INITIAL', 25, 25, NULL, 'Seed data', CAST(strftime('%s', 'now') AS INTEGER) * 1000),
    (2, 2, 'INITIAL', 10, 10, NULL, 'Seed data', CAST(strftime('%s', 'now') AS INTEGER) * 1000),
    (3, 3, 'INITIAL',  5,  5, NULL, 'Seed data', CAST(strftime('%s', 'now') AS INTEGER) * 1000),
    (4, 4, 'INITIAL',  3,  3, NULL, 'Seed data', CAST(strftime('%s', 'now') AS INTEGER) * 1000),
    (6, 6, 'INITIAL', 15, 15, NULL, 'Seed data', CAST(strftime('%s', 'now') AS INTEGER) * 1000),
    (7, 7, 'INITIAL',  8,  8, NULL, 'Seed data', CAST(strftime('%s', 'now') AS INTEGER) * 1000);
