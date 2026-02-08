# Location Service - Database Schema

## Overview

This document describes the database schema for the Location Service, focusing on JPA entity mappings and service-specific implementation details.

**Authoritative SQL Schema:** See [05-DataArchitecture.md](../../docs/05-DataArchitecture.md) for complete SQL definitions, including table creation statements, constraints, and indexes.

### Tables Owned by Location Service

| Table | Temporal | Description |
|-------|----------|-------------|
| `locations.locations` | Planned | Geospatial location data with PostGIS coordinates |
| `locations.locations_history` | - | Automatic history for locations (not yet implemented) |
| `locations.location_stats` | No | Video count per location |

The service uses Spring Data JPA with Hibernate Spatial for PostGIS integration.

---

## JPA Entity Mappings

All entities use Lombok `@Getter` and `@Setter` annotations to reduce boilerplate. Read-only fields use `@Setter(AccessLevel.NONE)`.

### Location Entity

```java
@Entity
@Table(name = "locations", schema = "locations")
@Getter
@Setter
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "geometry(Point, 4326)")
    private Point coordinates;  // org.locationtech.jts.geom.Point

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String country;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // Managed by PostgreSQL trigger - read-only in JPA
    @Setter(AccessLevel.NONE)
    @Column(name = "sys_period", insertable = false, updatable = false,
            columnDefinition = "tstzrange")
    private String sysPeriod;

    @OneToOne(mappedBy = "location", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private LocationStats stats;
}
```

**Notes:**
- `coordinates` uses JTS `Point` type with Hibernate Spatial for PostGIS integration
- SRID 4326 (WGS 84) is the standard for GPS coordinates (latitude/longitude)
- `sysPeriod` is read-only; currently set to `tstzrange(NOW(), NULL)` on insert via database default (versioning trigger not yet implemented)
- `sysPeriod` uses `String` type since the value is never accessed in Java
- `created_at` is explicit here (unlike user-service which derives it from `sys_period`)

### LocationStats Entity

```java
@Entity
@Table(name = "location_stats", schema = "locations")
@Getter
@Setter
public class LocationStats {

    @Id
    private UUID locationId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "video_count", nullable = false)
    private int videoCount = 0;
}
```

**Notes:**
- Uses `@MapsId` for shared primary key with Location
- Non-temporal: counters change on every video approval/deletion, history would explode storage
- Auto-created via database trigger when Location is inserted

---

## PostGIS Integration

### Hibernate Spatial Configuration

The service requires Hibernate Spatial for PostGIS support:

```groovy
// build.gradle
implementation "org.hibernate.orm:hibernate-spatial:${hibernateSpatialVersion}"
```

```yaml
# application.yml
spring:
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

### Working with Coordinates

```java
// Creating a Point from latitude/longitude
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

// Note: Point constructor takes (longitude, latitude) - X is longitude, Y is latitude
Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));

// Extracting coordinates from a Point
double latitude = point.getY();
double longitude = point.getX();
```

**Common mistake:** JTS `Coordinate` uses (x, y) order which maps to (longitude, latitude), not the more intuitive (latitude, longitude) order.

---

## Temporal vs Non-Temporal Decisions

| Table | Temporal | Rationale |
|-------|----------|-----------|
| `locations` | Planned | Audit trail for location edits; display name or address corrections should be tracked |
| `location_stats` | No | Counters change on every video approval - history would explode storage |

**Current state:** The `locations` table has a `sys_period` column but the versioning trigger and history table are not yet implemented. When implemented, this will follow the same pattern as user-service using a custom `locations.versioning_trigger_fn()` function.

**Storage implications:** Temporal tables roughly double write I/O and storage for tracked tables. History tables are append-only and grow indefinitely until archived.

**Accessing history:** Once implemented, history tables (`locations_history`) will be for manual SQL auditing only. The application will not expose history queries through APIs or services.

### Versioning Implementation (Planned)

When temporal history is implemented, it will use a custom versioning trigger function (similar to user-service):

```sql
CREATE OR REPLACE FUNCTION locations.versioning_trigger_fn()
RETURNS TRIGGER AS $$
DECLARE
    history_table TEXT;
BEGIN
    history_table := TG_ARGV[0];

    IF TG_OP = 'UPDATE' OR TG_OP = 'DELETE' THEN
        OLD.sys_period := tstzrange(lower(OLD.sys_period), NOW());
        EXECUTE format('INSERT INTO %s SELECT $1.*', history_table) USING OLD;
    END IF;

    IF TG_OP = 'UPDATE' THEN
        NEW.sys_period := tstzrange(NOW(), NULL);
        RETURN NEW;
    END IF;

    IF TG_OP = 'INSERT' THEN
        NEW.sys_period := tstzrange(NOW(), NULL);
        RETURN NEW;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;
```

This function will be applied via trigger:

```sql
CREATE TRIGGER locations_versioning_trigger
    BEFORE INSERT OR UPDATE OR DELETE ON locations.locations
    FOR EACH ROW EXECUTE FUNCTION locations.versioning_trigger_fn('locations.locations_history');
```

---

## Index Strategy

| Index | Column(s) | Type | Purpose |
|-------|-----------|------|---------|
| `idx_locations_coordinates` | `coordinates` | GIST | Spatial queries (bounding box, distance) |
| `idx_locations_city_state` | `city, state` | B-tree | Filter by city/state |
| `idx_locations_created_at` | `created_at` | B-tree | Sort by creation time |

**Guidance:** Don't add indexes speculatively. Each index slows writes and consumes storage. Add only when query patterns demand it.

---

## Common Query Patterns

### Find locations within bounding box

```java
@Query(nativeQuery = true, value = """
    SELECT l.* FROM locations.locations l
    WHERE ST_Within(
        l.coordinates,
        ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326)
    )
    ORDER BY l.created_at DESC
    LIMIT :limit
    """)
List<Location> findWithinBoundingBox(
    double minLng, double minLat,
    double maxLng, double maxLat,
    int limit
);
```

Uses `idx_locations_coordinates` GIST index for efficient spatial filtering.

### Find locations near a point

```java
@Query(nativeQuery = true, value = """
    SELECT l.*, ST_Distance(l.coordinates::geography, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography) as distance
    FROM locations.locations l
    WHERE ST_DWithin(
        l.coordinates::geography,
        ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
        :radiusMeters
    )
    ORDER BY distance
    LIMIT :limit
    """)
List<Location> findNearPoint(double lat, double lng, double radiusMeters, int limit);
```

**Note:** Cast to `geography` for accurate distance calculations in meters on the Earth's surface.

### Cluster locations for map display

```java
@Query(nativeQuery = true, value = """
    WITH clusters AS (
        SELECT
            ST_ClusterDBSCAN(coordinates, eps := :gridDegrees, minpoints := 1) OVER() as cluster_id,
            id,
            coordinates
        FROM locations.locations
        WHERE ST_Within(
            coordinates,
            ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326)
        )
    )
    SELECT
        cluster_id,
        ST_Centroid(ST_Collect(coordinates)) as centroid,
        COUNT(*) as count,
        ARRAY_AGG(id ORDER BY id LIMIT 5) as sample_ids
    FROM clusters
    GROUP BY cluster_id
    """)
List<Object[]> clusterLocations(
    double minLng, double minLat,
    double maxLng, double maxLat,
    double gridDegrees
);
```

Grid size (`gridDegrees`) should be calculated based on zoom level - smaller grid at higher zoom.

### Get location with video count

```java
@Query("""
    SELECT l FROM Location l
    LEFT JOIN FETCH l.stats
    WHERE l.id = :id
    """)
Optional<Location> findByIdWithStats(UUID id);
```

### Find locations by city/state

```java
List<Location> findByCityAndState(String city, String state);
```

Uses `idx_locations_city_state` composite index.

---

## Coordinate Validation

The database enforces coordinate validity via check constraint:

```sql
CONSTRAINT valid_coordinates CHECK (
    ST_X(coordinates) BETWEEN -180 AND 180 AND
    ST_Y(coordinates) BETWEEN -90 AND 90
)
```

The service layer should also validate coordinates before attempting to persist:

```java
public void validateCoordinates(double latitude, double longitude) {
    if (latitude < -90 || latitude > 90) {
        throw new IllegalArgumentException("Latitude must be between -90 and 90");
    }
    if (longitude < -180 || longitude > 180) {
        throw new IllegalArgumentException("Longitude must be between -180 and 180");
    }
}
```

---

## Migration Notes

- **Flyway naming:** `V{version}__{description}.sql` (e.g., `V1__create_locations_schema.sql`)
- **PostGIS extension:** Must be enabled before creating geometry columns
- **Spatial indexes:** Always use GIST index type for geometry columns
- **SRID consistency:** All geometry data uses SRID 4326 (WGS 84)
- **Testing migrations:** Run `./gradlew flywayMigrate` against local PostgreSQL with PostGIS before committing
