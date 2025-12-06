ALTER TABLE reservation
    ALTER COLUMN order_id TYPE VARCHAR(100) USING order_id::varchar(100);

