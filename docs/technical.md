# Location Service - Technical Documentation

## Service Overview

The Location Service manages all geospatial data for AccountabilityAtlas. It handles location storage, spatial queries for map display, marker clustering, and geocoding integration.

## Responsibilities

- Location storage with coordinates
- Bounding box queries for map viewport
- Marker clustering algorithm for efficient map display
- Geocoding (address → coordinates)
- Reverse geocoding (coordinates → address)
- Video count maintenance per location

## Technology Stack

| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 3.2.x |
| Language | Java 21 |
| Build | Gradle |
| Database | PostgreSQL 15 + PostGIS 3.3 |
| Cache | Redis |
| Geocoding | Google Maps Geocoding API |

## Dependencies

- **PostgreSQL + PostGIS**: Spatial data storage, geo queries
- **Redis**: Cluster caching, query result caching
- **Google Maps Geocoding API**: Address/coordinate conversion
- **SQS**: Event consumption (VideoApproved, VideoDeleted)

## Documentation Index

| Document | Status | Description |
|----------|--------|-------------|
| [api-specification.yaml](api-specification.yaml) | Complete | OpenAPI 3.1 specification |
| [database-schema.md](database-schema.md) | Planned | PostGIS schema documentation |
| [clustering-algorithm.md](clustering-algorithm.md) | Planned | Marker clustering implementation |
| [spatial-queries.md](spatial-queries.md) | Planned | PostGIS query patterns |

## Domain Model

```
Location (temporal - sys_period tracks history)
├── id: UUID
├── coordinates: Point  // PostGIS GEOGRAPHY(POINT, 4326)
├── displayName: String
├── address: String (nullable)
├── city: String (nullable)
├── state: String (nullable)
├── country: String (default: "USA")
└── sysPeriod: tstzrange  // lower bound = created, NULL upper = current

LocationStats (non-temporal - counters change frequently)
├── locationId: UUID
├── videoCount: int (denormalized)
└── updatedAt: Instant

MarkerCluster (clustering response)
├── id: String  // geohash or cluster id
├── coordinates: Point
├── count: int
├── sampleVideoIds: List<UUID>  // Up to 5 sample videos
└── bounds: BoundingBox
```

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /locations | Public | List locations in bounding box |
| GET | /locations/{id} | Public | Get location details |
| POST | /locations | User | Create new location |
| GET | /locations/cluster | Public | Get clustered markers |
| GET | /locations/geocode | User | Geocode address |
| GET | /locations/reverse | User | Reverse geocode coordinates |

## Query Parameters

### GET /locations
| Parameter | Type | Description |
|-----------|------|-------------|
| bbox | String | Bounding box: minLng,minLat,maxLng,maxLat |
| amendments | String[] | Filter by amendment types |
| limit | Int | Max results (default: 500) |

### GET /locations/cluster
| Parameter | Type | Description |
|-----------|------|-------------|
| bbox | String | Bounding box: minLng,minLat,maxLng,maxLat |
| zoom | Int | Map zoom level (1-20) |
| gridSize | Int | Clustering grid size in pixels (default: 60) |

## Clustering Algorithm

Server-side grid-based clustering using PostGIS:

```sql
-- Cluster locations within viewport
SELECT
    ST_ClusterDBSCAN(l.coordinates::geometry, eps := grid_size_degrees, minpoints := 1)
        OVER() as cluster_id,
    ST_Centroid(ST_Collect(l.coordinates::geometry)) as centroid,
    COUNT(*) as count,
    ARRAY_AGG(l.id ORDER BY ls.video_count DESC LIMIT 5) as sample_ids
FROM content.locations l
LEFT JOIN content.location_stats ls ON l.id = ls.location_id
WHERE ST_Intersects(
    l.coordinates::geometry,
    ST_MakeEnvelope(min_lng, min_lat, max_lng, max_lat, 4326)
)
GROUP BY cluster_id;
```

Cluster behavior by zoom level:
- Zoom 1-5: Large clusters (country/region level)
- Zoom 6-10: Medium clusters (state/city level)
- Zoom 11-15: Small clusters (neighborhood level)
- Zoom 16+: Individual markers (no clustering)

## Events Consumed

| Event | Action |
|-------|--------|
| VideoApproved | Increment location_stats.video_count for associated locations |
| VideoDeleted | Decrement location_stats.video_count for associated locations |

## Caching Strategy

| Cache Key | TTL | Purpose |
|-----------|-----|---------|
| `cluster:{bbox}:{zoom}` | 60s | Clustered markers |
| `location:{id}` | 300s | Location details |
| `geocode:{address_hash}` | 24h | Geocoding results |

## Local Development

```bash
# Start dependencies (uses PostGIS image)
docker-compose up -d postgres redis

# Run migrations
./gradlew flywayMigrate

# Set Google Maps API key
export GOOGLE_MAPS_API_KEY=your_key_here

# Run service
./gradlew bootRun

# Service available at http://localhost:8083
```
