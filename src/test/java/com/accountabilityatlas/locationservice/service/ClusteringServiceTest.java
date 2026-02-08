package com.accountabilityatlas.locationservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.accountabilityatlas.locationservice.domain.Location;
import com.accountabilityatlas.locationservice.repository.LocationRepository;
import com.accountabilityatlas.locationservice.service.ClusteringService.Cluster;
import com.accountabilityatlas.locationservice.service.ClusteringService.ClusterResult;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusteringServiceTest {

  private static final GeometryFactory GEOMETRY_FACTORY =
      new GeometryFactory(new PrecisionModel(), 4326);

  @Mock private LocationRepository locationRepository;

  @Mock private LocationService locationService;

  @InjectMocks private ClusteringService clusteringService;

  @Test
  void shouldReturnIndividualLocationsAtHighZoom() {
    int highZoom = 16;
    double minLng = -122.5;
    double minLat = 37.0;
    double maxLng = -122.0;
    double maxLat = 38.0;

    Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(-122.4194, 37.7749));
    Location location = Location.builder().coordinates(point).displayName("SF City Hall").build();

    when(locationService.getLocationsInBoundingBox(minLng, minLat, maxLng, maxLat))
        .thenReturn(List.of(location));

    ClusterResult result = clusteringService.getClusters(minLng, minLat, maxLng, maxLat, highZoom);

    assertThat(result.clusters()).isEmpty();
    assertThat(result.locations()).hasSize(1);
    assertThat(result.locations().get(0).getDisplayName()).isEqualTo("SF City Hall");

    verify(locationService).getLocationsInBoundingBox(minLng, minLat, maxLng, maxLat);
  }

  @Test
  void shouldReturnClustersAtLowZoom() {
    int lowZoom = 10;
    double minLng = -123.0;
    double minLat = 37.0;
    double maxLng = -122.0;
    double maxLat = 38.0;

    // Mock the raw cluster results from the repository
    // Each row: [lat, lng, count, clusterId]
    List<Object[]> rawClusters =
        List.of(
            new Object[] {37.7749, -122.4194, 5L, 0}, // cluster with 5 locations
            new Object[] {37.8044, -122.2712, 3L, 1}, // cluster with 3 locations
            new Object[] {37.6879, -122.4702, 1L, null} // single unclustered location
            );

    when(locationRepository.findClustersInBoundingBox(
            eq(minLng), eq(minLat), eq(maxLng), eq(maxLat), anyDouble()))
        .thenReturn(rawClusters);

    ClusterResult result = clusteringService.getClusters(minLng, minLat, maxLng, maxLat, lowZoom);

    assertThat(result.clusters()).hasSize(3);
    assertThat(result.locations()).isEmpty();

    // First cluster should have 5 locations and suggest zoom + 2
    Cluster firstCluster = result.clusters().get(0);
    assertThat(firstCluster.count()).isEqualTo(5);
    assertThat(firstCluster.latitude()).isEqualTo(37.7749);
    assertThat(firstCluster.longitude()).isEqualTo(-122.4194);
    assertThat(firstCluster.expansionZoom()).isEqualTo(12); // zoom + 2

    // Second cluster should have 3 locations
    Cluster secondCluster = result.clusters().get(1);
    assertThat(secondCluster.count()).isEqualTo(3);
    assertThat(secondCluster.expansionZoom()).isEqualTo(12);

    // Third is a single location (no cluster), should have null expansion zoom
    Cluster thirdCluster = result.clusters().get(2);
    assertThat(thirdCluster.count()).isEqualTo(1);
    assertThat(thirdCluster.expansionZoom()).isNull();
  }

  @Test
  void shouldReturnEmptyClustersForEmptyBoundingBox() {
    int zoom = 10;
    double minLng = -123.0;
    double minLat = 37.0;
    double maxLng = -122.0;
    double maxLat = 38.0;

    when(locationRepository.findClustersInBoundingBox(
            eq(minLng), eq(minLat), eq(maxLng), eq(maxLat), anyDouble()))
        .thenReturn(Collections.emptyList());

    ClusterResult result = clusteringService.getClusters(minLng, minLat, maxLng, maxLat, zoom);

    assertThat(result.clusters()).isEmpty();
    assertThat(result.locations()).isEmpty();
  }

  @Test
  void shouldCalculateEpsilonCorrectly() {
    // At zoom 1, epsilon should be ~180 degrees (half world)
    double epsilonZoom1 = clusteringService.calculateEpsilon(1);
    assertThat(epsilonZoom1).isCloseTo(180.0, org.assertj.core.api.Assertions.within(0.1));

    // At zoom 10, epsilon should be ~0.35 degrees
    double epsilonZoom10 = clusteringService.calculateEpsilon(10);
    assertThat(epsilonZoom10).isCloseTo(0.3515625, org.assertj.core.api.Assertions.within(0.001));

    // At zoom 15, epsilon should be very small (~0.01 degrees)
    double epsilonZoom15 = clusteringService.calculateEpsilon(15);
    assertThat(epsilonZoom15).isCloseTo(0.01098633, org.assertj.core.api.Assertions.within(0.0001));

    // Higher zoom = smaller epsilon
    assertThat(epsilonZoom10).isLessThan(epsilonZoom1);
    assertThat(epsilonZoom15).isLessThan(epsilonZoom10);
  }

  @Test
  void shouldCalculateEpsilonWithFormula360DividedBy2ToThePowerOfZoom() {
    for (int zoom = 1; zoom <= 20; zoom++) {
      double expectedEpsilon = 360.0 / Math.pow(2, zoom);
      double actualEpsilon = clusteringService.calculateEpsilon(zoom);
      assertThat(actualEpsilon).isEqualTo(expectedEpsilon);
    }
  }
}
