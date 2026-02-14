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
    // Each row: [lat, lng, count, clusterId, minLat, maxLat, minLng, maxLng]
    List<Object[]> rawClusters =
        List.of(
            new Object[] {37.7749, -122.4194, 5L, 0, 37.5, 38.0, -122.5, -122.3},
            new Object[] {37.8044, -122.2712, 3L, 1, 37.7, 37.9, -122.4, -122.1},
            new Object[] {37.6879, -122.4702, 1L, null, 37.6879, 37.6879, -122.4702, -122.4702});

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
    assertThat(firstCluster.minLat()).isEqualTo(37.5);
    assertThat(firstCluster.maxLat()).isEqualTo(38.0);
    assertThat(firstCluster.minLng()).isEqualTo(-122.5);
    assertThat(firstCluster.maxLng()).isEqualTo(-122.3);

    // Second cluster should have 3 locations
    Cluster secondCluster = result.clusters().get(1);
    assertThat(secondCluster.count()).isEqualTo(3);
    assertThat(secondCluster.expansionZoom()).isEqualTo(12);
    assertThat(secondCluster.minLat()).isEqualTo(37.7);
    assertThat(secondCluster.maxLat()).isEqualTo(37.9);

    // Third is a single location (no cluster), should have null expansion zoom
    Cluster thirdCluster = result.clusters().get(2);
    assertThat(thirdCluster.count()).isEqualTo(1);
    assertThat(thirdCluster.expansionZoom()).isNull();
    // Single location bounds should be the point itself
    assertThat(thirdCluster.minLat()).isEqualTo(37.6879);
    assertThat(thirdCluster.maxLat()).isEqualTo(37.6879);
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
    // At zoom 1, epsilon should be 22.5 degrees (1/8 viewport)
    double epsilonZoom1 = clusteringService.calculateEpsilon(1);
    assertThat(epsilonZoom1).isCloseTo(22.5, org.assertj.core.api.Assertions.within(0.1));

    // At zoom 10, epsilon should be ~0.044 degrees
    double epsilonZoom10 = clusteringService.calculateEpsilon(10);
    assertThat(epsilonZoom10)
        .isCloseTo(0.0439453125, org.assertj.core.api.Assertions.within(0.001));

    // At zoom 15, epsilon should be very small (~0.00137 degrees)
    double epsilonZoom15 = clusteringService.calculateEpsilon(15);
    assertThat(epsilonZoom15)
        .isCloseTo(0.001373291, org.assertj.core.api.Assertions.within(0.0001));

    // Higher zoom = smaller epsilon
    assertThat(epsilonZoom10).isLessThan(epsilonZoom1);
    assertThat(epsilonZoom15).isLessThan(epsilonZoom10);
  }

  @Test
  void shouldCalculateEpsilonWithFormula45DividedBy2ToThePowerOfZoom() {
    for (int zoom = 1; zoom <= 20; zoom++) {
      double expectedEpsilon = 45.0 / Math.pow(2, zoom);
      double actualEpsilon = clusteringService.calculateEpsilon(zoom);
      assertThat(actualEpsilon).isEqualTo(expectedEpsilon);
    }
  }
}
