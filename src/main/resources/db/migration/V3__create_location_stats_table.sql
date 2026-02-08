-- Separate non-temporal table for frequently updated counters
CREATE TABLE locations.location_stats (
    location_id UUID PRIMARY KEY REFERENCES locations.locations(id) ON DELETE CASCADE,
    video_count INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT non_negative_video_count CHECK (video_count >= 0)
);

-- Function to auto-create stats row when location is created
CREATE OR REPLACE FUNCTION locations.create_location_stats()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO locations.location_stats (location_id, video_count)
    VALUES (NEW.id, 0);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_create_location_stats
    AFTER INSERT ON locations.locations
    FOR EACH ROW
    EXECUTE FUNCTION locations.create_location_stats();
