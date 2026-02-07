package com.accountabilityatlas.locationservice.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.accountabilityatlas.locationservice.domain.Location;
import com.accountabilityatlas.locationservice.repository.LocationRepository;
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
    // Create location inside bbox (San Francisco)
    Point sfPoint = GEOMETRY_FACTORY.createPoint(new Coordinate(-122.4194, 37.7749));
    Location sfLocation =
        Location.builder()
            .coordinates(sfPoint)
            .displayName("San Francisco")
            .city("San Francisco")
            .state("CA")
            .country("USA")
            .build();
    locationRepository.save(sfLocation);

    // Create location outside bbox (Los Angeles)
    Point laPoint = GEOMETRY_FACTORY.createPoint(new Coordinate(-118.2437, 34.0522));
    Location laLocation =
        Location.builder()
            .coordinates(laPoint)
            .displayName("Los Angeles")
            .city("Los Angeles")
            .state("CA")
            .country("USA")
            .build();
    locationRepository.save(laLocation);

    // Bounding box around San Francisco only
    Polygon bbox =
        GEOMETRY_FACTORY.createPolygon(
            new Coordinate[] {
              new Coordinate(-123.0, 37.0),
              new Coordinate(-122.0, 37.0),
              new Coordinate(-122.0, 38.0),
              new Coordinate(-123.0, 38.0),
              new Coordinate(-123.0, 37.0)
            });

    List<Location> results = locationRepository.findWithinBoundingBox(bbox);

    assertThat(results).hasSize(1);
    assertThat(results.get(0).getDisplayName()).isEqualTo("San Francisco");
  }
}
