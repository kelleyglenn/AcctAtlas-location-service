CREATE SCHEMA IF NOT EXISTS locations;

-- Enable PostGIS extension (must be done by superuser or in shared_preload_libraries)
CREATE EXTENSION IF NOT EXISTS postgis;
