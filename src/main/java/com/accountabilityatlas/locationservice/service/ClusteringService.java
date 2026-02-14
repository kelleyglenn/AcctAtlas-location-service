package com.accountabilityatlas.locationservice.service;

import com.accountabilityatlas.locationservice.domain.Location;
import com.accountabilityatlas.locationservice.repository.LocationRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClusteringService {

  private static final int HIGH_ZOOM_THRESHOLD = 15;

  private final LocationRepository locationRepository;
  private final LocationService locationService;

  /**
   * Calculates epsilon (clustering distance in degrees) based on zoom level. Higher zoom = smaller
   * epsilon = fewer/smaller clusters.
   *
   * @param zoom the map zoom level (1-20)
   * @return the epsilon value in degrees
   */
  double calculateEpsilon(int zoom) {
    // Formula: 45 / 2^zoom (1/8th of viewport width in degrees)
    // zoom 1 (world): 22.5° - regional clusters
    // zoom 5 (continent): ~1.4°
    // zoom 10 (region): ~0.044° (~4.9 km at equator)
    // zoom 15 (street): ~0.00137° (~150 m at equator)
    // zoom > 15: bypassed, returns individual locations (see HIGH_ZOOM_THRESHOLD)
    return 45.0 / Math.pow(2, zoom);
  }

  /**
   * Gets clustered locations for a bounding box at a given zoom level.
   *
   * @param minLng minimum longitude of the bounding box
   * @param minLat minimum latitude of the bounding box
   * @param maxLng maximum longitude of the bounding box
   * @param maxLat maximum latitude of the bounding box
   * @param zoom the map zoom level (1-20)
   * @return a ClusterResult containing clusters and individual locations
   */
  @Transactional(readOnly = true)
  public ClusterResult getClusters(
      double minLng, double minLat, double maxLng, double maxLat, int zoom) {
    // At high zoom, return individual locations instead of clusters
    if (zoom > HIGH_ZOOM_THRESHOLD) {
      List<Location> locations =
          locationService.getLocationsInBoundingBox(minLng, minLat, maxLng, maxLat);
      return new ClusterResult(Collections.emptyList(), locations);
    }

    double eps = calculateEpsilon(zoom);
    List<Object[]> rawClusters =
        locationRepository.findClustersInBoundingBox(minLng, minLat, maxLng, maxLat, eps);

    List<Cluster> clusters = new ArrayList<>();
    List<Location> singleLocations = new ArrayList<>();

    for (Object[] row : rawClusters) {
      double lat = ((Number) row[0]).doubleValue();
      double lng = ((Number) row[1]).doubleValue();
      int count = ((Number) row[2]).intValue();
      Integer clusterId = row[3] != null ? ((Number) row[3]).intValue() : null;
      double boundsMinLat = ((Number) row[4]).doubleValue();
      double boundsMaxLat = ((Number) row[5]).doubleValue();
      double boundsMinLng = ((Number) row[6]).doubleValue();
      double boundsMaxLng = ((Number) row[7]).doubleValue();

      if (clusterId != null || count > 1) {
        // Real cluster - suggest zooming in 2 levels to see individual markers
        clusters.add(
            new Cluster(
                lat, lng, count, zoom + 2, boundsMinLat, boundsMaxLat, boundsMinLng, boundsMaxLng));
      } else {
        // Single location - return as single-point cluster with no expansion zoom
        clusters.add(
            new Cluster(lat, lng, 1, null, boundsMinLat, boundsMaxLat, boundsMinLng, boundsMaxLng));
      }
    }

    return new ClusterResult(clusters, singleLocations);
  }

  public record Cluster(
      double latitude,
      double longitude,
      int count,
      Integer expansionZoom,
      double minLat,
      double maxLat,
      double minLng,
      double maxLng) {}

  public record ClusterResult(List<Cluster> clusters, List<Location> locations) {}
}
