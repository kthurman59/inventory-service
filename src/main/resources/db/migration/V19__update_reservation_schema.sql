SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'reservation';

SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'reservation_line';

ALTER TABLE reservation
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ;

UPDATE reservation
SET created_at = NOW(),
    updated_at = NOW()
WHERE created_at IS NULL
   OR updated_at IS NULL;

ALTER TABLE reservation
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

