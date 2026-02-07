-- Add created_at column (defaults to current timestamp for existing records)
ALTER TABLE locations.locations
ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

-- Index for sorting by creation time
CREATE INDEX idx_locations_created_at ON locations.locations(created_at);
