ALTER TABLE inventory_items
    ADD COLUMN IF NOT EXISTS sku VARCHAR(64) NOT NULL,
    ADD COLUMN IF NOT EXISTS location_id VARCHAR(64) NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_inventory_sku_location
    ON inventory_items(sku, location_id);

