ALTER TABLE stock_adjustment
    ALTER COLUMN location_id TYPE VARCHAR(50)
    USING location_id::varchar(50);
