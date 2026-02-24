package com.accountabilityatlas.locationservice.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.accountabilityatlas.locationservice.domain.Location;
import com.accountabilityatlas.locationservice.domain.LocationStats;
import com.accountabilityatlas.locationservice.repository.LocationRepository;
import com.accountabilityatlas.locationservice.repository.LocationStatsRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LocationRepositoryIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>(
              DockerImageName.parse("postgis/postgis:17-3.5-alpine")
                  .asCompatibleSubstituteFor("postgres"))
          .withDatabaseName("location_service")
          .withUsername("test")
          .withPassword("test");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.flyway.url", postgres::getJdbcUrl);
    registry.add("spring.flyway.user", postgres::getUsername);
    registry.add("spring.flyway.password", postgres::getPassword);
  }

  private static final GeometryFactory GEOMETRY_FACTORY =
      new GeometryFactory(new PrecisionModel(), 4326);

  @Autowired private LocationRepository locationRepository;
  @Autowired private LocationStatsRepository locationStatsRepository;
  @Autowired private TestEntityManager entityManager;

  @BeforeEach
  void setUp() {
    locationRepository.deleteAll();
  }

  @Test
  void shouldSaveAndRetrieveLocation() {
    Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(-122.4194, 37.7749));
    Location location =
        Location.builder()
            .coordinates(point)
            .displayName("San Francisco City Hall")
            .city("San Francisco")
            .state("CA")
            .country("USA")
            .build();

    Location saved = locationRepository.save(location);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getDisplayName()).isEqualTo("San Francisco City Hall");
    assertThat(saved.getLatitude()).isEqualTo(37.7749);
    assertThat(saved.getLongitude()).isEqualTo(-122.4194);
  }

  @Test
  void shouldFindLocationsWithinBoundingBox() {
    // Arrange — location inside bbox with video_count > 0
    saveLocationWithVideoCount("San Francisco", -122.4194, 37.7749, 1);

    // Arrange — location outside bbox with video_count > 0
    saveLocationWithVideoCount("Los Angeles", -118.2437, 34.0522, 1);

    // Act
    Polygon bbox = createBbox(-123.0, 37.0, -122.0, 38.0);
    List<Location> results = locationRepository.findWithinBoundingBox(bbox);

    // Assert
    assertThat(results).hasSize(1);
    assertThat(results.getFirst().getDisplayName()).isEqualTo("San Francisco");
  }

  @Test
  void shouldExcludeLocationsWithZeroVideoCount() {
    // Arrange — location inside bbox but with video_count = 0
    saveLocationWithVideoCount("Empty Location", -122.4194, 37.7749, 0);

    // Act
    Polygon bbox = createBbox(-123.0, 37.0, -122.0, 38.0);
    List<Location> results = locationRepository.findWithinBoundingBox(bbox);

    // Assert
    assertThat(results).isEmpty();
  }

  private Location saveLocationWithVideoCount(
      String displayName, double lng, double lat, int videoCount) {
    Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));
    Location location =
        Location.builder()
            .coordinates(point)
            .displayName(displayName)
            .city(displayName)
            .state("CA")
            .country("USA")
            .build();
    location = locationRepository.saveAndFlush(location);

    // Database trigger auto-creates location_stats with video_count=0;
    // update the count to the desired value
    LocationStats stats = locationStatsRepository.findById(location.getId()).orElseThrow();
    stats.setVideoCount(videoCount);
    locationStatsRepository.saveAndFlush(stats);

    entityManager.clear();

    return location;
  }

  private Polygon createBbox(double minLng, double minLat, double maxLng, double maxLat) {
    return GEOMETRY_FACTORY.createPolygon(
        new Coordinate[] {
          new Coordinate(minLng, minLat),
          new Coordinate(maxLng, minLat),
          new Coordinate(maxLng, maxLat),
          new Coordinate(minLng, maxLat),
          new Coordinate(minLng, minLat)
        });
  }
}
