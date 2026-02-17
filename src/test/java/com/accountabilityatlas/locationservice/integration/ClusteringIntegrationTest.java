package com.accountabilityatlas.locationservice.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.accountabilityatlas.locationservice.domain.Location;
import com.accountabilityatlas.locationservice.repository.LocationRepository;
import com.accountabilityatlas.locationservice.service.ClusteringService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
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
class ClusteringIntegrationTest {

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

  @PersistenceContext private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    locationRepository.deleteAll();

    // Bay Area locations (5 points, close together)
    saveLocation(-122.4194, 37.7749, "San Francisco City Hall");
    saveLocation(-122.2712, 37.8044, "Oakland City Hall");
    saveLocation(-121.8906, 37.3382, "San Jose City Hall");
    saveLocation(-122.4098, 37.8716, "Berkeley City Hall");
    saveLocation(-122.0322, 37.5585, "Fremont City Hall");

    // Texas locations (2 points, close together)
    saveLocation(-97.7431, 30.2672, "Austin Capitol");
    saveLocation(-97.6031, 30.4083, "Round Rock");

    // Colorado (isolated point)
    saveLocation(-106.0553, 39.6335, "Silverthorne");

    // Set video_count = 1 for all locations so cluster query includes them
    entityManager
        .createNativeQuery("UPDATE locations.location_stats SET video_count = 1")
        .executeUpdate();
    entityManager.flush();
  }

  @Test
  void shouldReturnMultipleClustersAtZoom4WithFixedEpsilon() {
    // epsilon at zoom 4 = 45 / 2^4 = 2.8125°
    // Bay Area points span ~0.7° lat, ~0.5° lng → should cluster together
    // Texas points span ~0.14° → should cluster together
    // Colorado is isolated → individual marker
    // Bay Area centroid is ~(37.67, -122.2) and Texas centroid is ~(30.34, -97.67)
    // These are ~7° apart in latitude, far beyond epsilon → separate clusters
    double eps = ClusteringService.calculateEpsilon(4); // 2.8125°

    List<Object[]> results =
        locationRepository.findClustersInBoundingBox(-130.0, 24.0, -60.0, 50.0, eps);

    // Should NOT be one giant cluster; expect at least 2 distinct groups
    assertThat(results).hasSizeGreaterThanOrEqualTo(2);

    // Count total locations across all results
    int totalCount = results.stream().mapToInt(row -> ((Number) row[2]).intValue()).sum();
    assertThat(totalCount).isEqualTo(8);
  }

  @Test
  void shouldReturnBoundsForBayAreaCluster() {
    // At zoom 6, epsilon = 45 / 2^6 = 0.703125°
    // Bay Area points span ~0.53° lat → should cluster together
    // Query just the Bay Area region
    double eps = ClusteringService.calculateEpsilon(6);

    List<Object[]> results =
        locationRepository.findClustersInBoundingBox(-123.0, 37.0, -121.0, 38.5, eps);

    // Find the cluster with count > 1 (the Bay Area cluster)
    Object[] bayAreaCluster = null;
    for (Object[] row : results) {
      int count = ((Number) row[2]).intValue();
      if (count > 1) {
        bayAreaCluster = row;
        break;
      }
    }

    assertThat(bayAreaCluster).isNotNull();

    // Verify bounds columns are present (indices 4-7)
    double minLat = ((Number) bayAreaCluster[4]).doubleValue();
    double maxLat = ((Number) bayAreaCluster[5]).doubleValue();
    double minLng = ((Number) bayAreaCluster[6]).doubleValue();
    double maxLng = ((Number) bayAreaCluster[7]).doubleValue();

    // San Jose is the southernmost (37.3382), Berkeley is northernmost (37.8716)
    assertThat(minLat).isCloseTo(37.3382, within(0.01));
    assertThat(maxLat).isCloseTo(37.8716, within(0.01));

    // Fremont is the easternmost (-122.0322), SF is westernmost (-122.4194)
    // Note: for western hemisphere, min lng is the more negative value
    assertThat(minLng).isCloseTo(-122.4194, within(0.01));
    assertThat(maxLng).isCloseTo(-121.8906, within(0.01));

    // Verify centroid is within bounds
    double centroidLat = ((Number) bayAreaCluster[0]).doubleValue();
    double centroidLng = ((Number) bayAreaCluster[1]).doubleValue();
    assertThat(centroidLat).isBetween(minLat, maxLat);
    assertThat(centroidLng).isBetween(minLng, maxLng);
  }

  @Test
  void shouldReturnAccurateBayAreaCentroid() {
    // At zoom 5, epsilon = 45/32 = 1.40625 - all Bay Area points should cluster
    double eps = ClusteringService.calculateEpsilon(5);

    List<Object[]> results =
        locationRepository.findClustersInBoundingBox(-123.0, 37.0, -121.0, 38.5, eps);

    // Find the cluster with all 5 Bay Area locations
    Object[] bayAreaCluster = null;
    for (Object[] row : results) {
      int count = ((Number) row[2]).intValue();
      if (count == 5) {
        bayAreaCluster = row;
        break;
      }
    }

    assertThat(bayAreaCluster).isNotNull();

    double centroidLat = ((Number) bayAreaCluster[0]).doubleValue();
    double centroidLng = ((Number) bayAreaCluster[1]).doubleValue();

    // Expected centroid: arithmetic mean of coordinates
    // SF(37.7749), Oakland(37.8044), SanJose(37.3382), Berkeley(37.8716), Fremont(37.5585)
    //   -> avg 37.6695
    // SF(-122.4194), Oakland(-122.2712), SanJose(-121.8906), Berkeley(-122.4098),
    //   Fremont(-122.0322) -> avg -122.2046
    assertThat(centroidLat).isCloseTo(37.67, within(0.1));
    assertThat(centroidLng).isCloseTo(-122.20, within(0.1));
  }

  private void saveLocation(double lng, double lat, String name) {
    Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));
    Location location =
        Location.builder()
            .coordinates(point)
            .displayName(name)
            .city(name)
            .state("XX")
            .country("USA")
            .build();
    locationRepository.save(location);
  }
}
