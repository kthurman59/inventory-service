CREATE TABLE inventory_item (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(100) NOT NULL,
    location_id VARCHAR(50) NOT NULL,
    on_hand_quantity BIGINT NOT NULL,
    reserved_quantity BIGINT NOT NULL,
    safety_stock BIGINT,
    reorder_point BIGINT,
    version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX ux_inventory_item_sku_location
    ON inventory_item (sku, location_id);

CREATE TABLE reservation (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX ix_reservation_order_id
    ON reservation (order_id);

CREATE TABLE reservation_line (
    id BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    sku VARCHAR(100) NOT NULL,
    location_id VARCHAR(50) NOT NULL,
    requested_quantity BIGINT NOT NULL,
    reserved_quantity BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    failure_reason TEXT,
    CONSTRAINT fk_reservation_line_reservation
        FOREIGN KEY (reservation_id)
            REFERENCES reservation (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_reservation_line_inventory_item
        FOREIGN KEY (inventory_item_id)
            REFERENCES inventory_item (id)
            ON DELETE RESTRICT
);

CREATE INDEX ix_reservation_line_reservation_id
    ON reservation_line (reservation_id);

CREATE INDEX ix_reservation_line_sku_location
    ON reservation_line (sku, location_id);

CREATE TABLE stock_adjustment (
    id BIGSERIAL PRIMARY KEY,
    inventory_item_id BIGINT NOT NULL,
    sku VARCHAR(100) NOT NULL,
    location_id VARCHAR(50) NOT NULL,
    quantity_delta BIGINT NOT NULL,
    reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100),
    CONSTRAINT fk_stock_adjustment_inventory_item
        FOREIGN KEY (inventory_item_id)
            REFERENCES inventory_item (id)
            ON DELETE RESTRICT
);

CREATE INDEX ix_stock_adjustment_inventory_item
    ON stock_adjustment (inventory_item_id);

