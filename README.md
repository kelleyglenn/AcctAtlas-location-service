# AcctAtlas Location Service

Geospatial data management service for AccountabilityAtlas. Handles location storage, bounding box queries for map display, marker clustering, and geocoding integration via Mapbox.

## Prerequisites

- **Docker Desktop** (for PostgreSQL + PostGIS)
- **Git**
- **Mapbox Access Token** (for geocoding features)

JDK 21 is managed automatically by the Gradle wrapper via [Foojay Toolchain](https://github.com/gradle/foojay-toolchain) -- no manual JDK installation required.

## Clone and Build

```bash
git clone <repo-url>
cd AcctAtlas-location-service
```

Build the project (downloads JDK 21 automatically on first run):

```bash
# Linux/macOS
./gradlew build

# Windows
gradlew.bat build
```

## Local Development

### Start dependencies

```bash
docker-compose up -d
```

This starts PostgreSQL 17 with PostGIS 3.5. Flyway migrations run automatically when the service starts.

### Set environment variables

```bash
# Required for geocoding features
export MAPBOX_ACCESS_TOKEN=your_token_here
```

### Run the service

```bash
# Linux/macOS
./gradlew bootRun

# Windows
gradlew.bat bootRun
```

The service starts on **http://localhost:8083**.

### Quick API test

```bash
# Health check
curl http://localhost:8083/actuator/health

# Create a location (requires auth token from user-service)
curl -X POST http://localhost:8083/locations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"coordinates":{"latitude":37.7749,"longitude":-122.4194},"displayName":"San Francisco City Hall","city":"San Francisco","state":"CA","country":"USA"}'

# List locations in bounding box (San Francisco area)
curl "http://localhost:8083/locations?bbox=-122.5,37.5,-122.0,38.0"

# Get clustered markers for map display
curl "http://localhost:8083/locations/cluster?bbox=-122.5,37.5,-122.0,38.0&zoom=10"

# Get location by ID (replace ID with actual UUID)
curl http://localhost:8083/locations/550e8400-e29b-41d4-a716-446655440000
```

### Run tests

```bash
./gradlew test
```

Integration tests use [TestContainers](https://testcontainers.com/) to spin up PostgreSQL with PostGIS automatically -- Docker must be running.

### Code formatting

Formatting is enforced by [Spotless](https://github.com/diffplug/spotless) using Google Java Format.

```bash
# Check formatting
./gradlew spotlessCheck

# Auto-fix formatting
./gradlew spotlessApply
```

### Full quality check

Runs Spotless, Error Prone, tests, and JaCoCo coverage verification (80% minimum):

```bash
./gradlew check
```

## Docker Image

Build a Docker image locally using [Jib](https://github.com/GoogleContainerTools/jib) (no Dockerfile needed):

```bash
./gradlew jibDockerBuild
```

Build and start the full stack (service + Postgres) in Docker:

```bash
./gradlew composeUp
```

## Project Structure

```
src/main/java/com/accountabilityatlas/locationservice/
  config/        Spring configuration (JPA, PostGIS, Security)
  domain/        JPA entities (Location, LocationStats)
  repository/    Spring Data JPA repositories with spatial queries
  service/       Business logic (clustering, geocoding)
  web/           Controller implementations

src/main/resources/
  application.yml          Shared config
  application-local.yml    Local dev overrides
  db/migration/            Flyway SQL migrations

src/test/java/.../
  domain/        Entity unit tests
  service/       Service unit tests (Mockito)
  web/           Controller tests (@WebMvcTest)
  integration/   Integration tests (TestContainers)
```

API interfaces and DTOs are generated from `docs/api-specification.yaml` by the OpenAPI Generator plugin into `build/generated/`.

## Key Gradle Tasks

| Task | Description |
|------|-------------|
| `bootRun` | Run the service locally (uses `local` profile) |
| `test` | Run all tests |
| `unitTest` | Run unit tests only (no Docker required) |
| `integrationTest` | Run integration tests only (requires Docker) |
| `check` | Full quality gate (format + analysis + tests + coverage) |
| `spotlessApply` | Auto-fix code formatting |
| `jibDockerBuild` | Build Docker image |
| `composeUp` | Build image + docker-compose up |
| `composeDown` | Stop docker-compose services |

## Documentation

- [Technical Overview](docs/technical.md)
- [API Specification](docs/api-specification.yaml) (OpenAPI 3.1)
- [Database Schema](docs/database-schema.md)
