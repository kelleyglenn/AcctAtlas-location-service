CREATE TABLE locations.locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    coordinates GEOMETRY(Point, 4326) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    sys_period tstzrange NOT NULL DEFAULT tstzrange(NOW(), NULL),

    CONSTRAINT valid_coordinates CHECK (
        ST_X(coordinates) BETWEEN -180 AND 180 AND
        ST_Y(coordinates) BETWEEN -90 AND 90
    )
);

-- Spatial index for bounding box queries
CREATE INDEX idx_locations_coordinates ON locations.locations USING GIST(coordinates);

-- Index for city/state queries
CREATE INDEX idx_locations_city_state ON locations.locations(city, state);
