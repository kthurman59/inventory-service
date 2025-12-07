ALTER TABLE inventory_items
    ADD COLUMN sku VARCHAR(64) NOT NULL;

ALTER TABLE inventory_items
    ADD COLUMN location_id VARCHAR(64) NOT NULL;
