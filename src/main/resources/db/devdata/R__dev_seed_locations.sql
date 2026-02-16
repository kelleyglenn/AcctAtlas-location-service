-- Dev seed data: Test locations for local development
-- 5 locations in San Francisco Bay Area, 5 scattered across USA
-- UUIDs match those referenced in video-service video_locations

-- Insert locations using PostGIS geometry
-- Note: ST_MakePoint takes (longitude, latitude) order
INSERT INTO locations.locations (id, coordinates, display_name, city, state, country) VALUES
    -- San Francisco Bay Area (5 locations)
    ('20000000-0000-0000-0000-000000000001',
     ST_SetSRID(ST_MakePoint(-122.4193, 37.7793), 4326),
     'San Francisco City Hall', 'San Francisco', 'CA', 'USA'),

    ('20000000-0000-0000-0000-000000000002',
     ST_SetSRID(ST_MakePoint(-122.2712, 37.8044), 4326),
     'Oakland Federal Building', 'Oakland', 'CA', 'USA'),

    ('20000000-0000-0000-0000-000000000003',
     ST_SetSRID(ST_MakePoint(-121.8863, 37.3382), 4326),
     'San Jose Police HQ', 'San Jose', 'CA', 'USA'),

    ('20000000-0000-0000-0000-000000000004',
     ST_SetSRID(ST_MakePoint(-121.9886, 37.5485), 4326),
     'Fremont City Hall', 'Fremont', 'CA', 'USA'),

    ('20000000-0000-0000-0000-000000000005',
     ST_SetSRID(ST_MakePoint(-122.2727, 37.8716), 4326),
     'Berkeley Post Office', 'Berkeley', 'CA', 'USA'),

    -- Scattered across USA (5 locations)
    ('20000000-0000-0000-0000-000000000006',
     ST_SetSRID(ST_MakePoint(-98.4936, 29.4241), 4326),
     'San Antonio Strip Mall', 'San Antonio', 'TX', 'USA'),

    ('20000000-0000-0000-0000-000000000007',
     ST_SetSRID(ST_MakePoint(-98.6136, 29.4952), 4326),
     'Leon Valley Police Department', 'Leon Valley', 'TX', 'USA'),

    ('20000000-0000-0000-0000-000000000008',
     ST_SetSRID(ST_MakePoint(-106.0753, 39.6336), 4326),
     'Silverthorne Post Office', 'Silverthorne', 'CO', 'USA'),

    ('20000000-0000-0000-0000-000000000009',
     ST_SetSRID(ST_MakePoint(-84.4839, 42.7370), 4326),
     'East Lansing Police Department', 'East Lansing', 'MI', 'USA'),

    ('20000000-0000-0000-0000-000000000010',
     ST_SetSRID(ST_MakePoint(-90.9712, 36.2612), 4326),
     'Pocahontas City Hall', 'Pocahontas', 'AR', 'USA')
ON CONFLICT (id) DO UPDATE SET
    coordinates = EXCLUDED.coordinates,
    display_name = EXCLUDED.display_name,
    city = EXCLUDED.city,
    state = EXCLUDED.state,
    country = EXCLUDED.country;

-- Insert location_stats entries
INSERT INTO locations.location_stats (location_id, video_count)
VALUES
    ('20000000-0000-0000-0000-000000000001', 1),
    ('20000000-0000-0000-0000-000000000002', 1),
    ('20000000-0000-0000-0000-000000000003', 1),
    ('20000000-0000-0000-0000-000000000004', 1),
    ('20000000-0000-0000-0000-000000000005', 1),
    ('20000000-0000-0000-0000-000000000006', 1),
    ('20000000-0000-0000-0000-000000000007', 1),
    ('20000000-0000-0000-0000-000000000008', 1),
    ('20000000-0000-0000-0000-000000000009', 1),
    ('20000000-0000-0000-0000-000000000010', 1)
ON CONFLICT (location_id) DO UPDATE SET video_count = EXCLUDED.video_count;
