package com.accountabilityatlas.locationservice.web;

import com.accountabilityatlas.locationservice.domain.Location;
import com.accountabilityatlas.locationservice.service.ClusteringService;
import com.accountabilityatlas.locationservice.service.ClusteringService.Cluster;
import com.accountabilityatlas.locationservice.service.ClusteringService.ClusterResult;
import com.accountabilityatlas.locationservice.web.api.ClusteringApi;
import com.accountabilityatlas.locationservice.web.model.BoundingBox;
import com.accountabilityatlas.locationservice.web.model.ClusterResponse;
import com.accountabilityatlas.locationservice.web.model.Coordinates;
import com.accountabilityatlas.locationservice.web.model.MarkerCluster;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ClusteringController implements ClusteringApi {

  private final ClusteringService clusteringService;

  @Override
  public ResponseEntity<ClusterResponse> getClusteredMarkers(
      String bbox, Integer zoom, Integer gridSize) {
    double[] coords = parseBoundingBox(bbox);

    ClusterResult result =
        clusteringService.getClusters(coords[0], coords[1], coords[2], coords[3], zoom);

    List<MarkerCluster> markerClusters = new ArrayList<>();
    int totalLocations = 0;

    // Convert service clusters to API model
    for (Cluster cluster : result.clusters()) {
      String id = generateClusterId(cluster.latitude(), cluster.longitude(), zoom);
      BoundingBox boundingBox =
          new BoundingBox(cluster.minLat(), cluster.minLng(), cluster.maxLat(), cluster.maxLng());
      MarkerCluster markerCluster =
          new MarkerCluster()
              .id(id)
              .coordinates(
                  new Coordinates().latitude(cluster.latitude()).longitude(cluster.longitude()))
              .count(cluster.count())
              .sampleVideoIds(new ArrayList<>())
              .bounds(boundingBox);

      markerClusters.add(markerCluster);
      totalLocations += cluster.count();
    }

    // Convert individual locations (at high zoom) to single-point clusters
    for (Location location : result.locations()) {
      String id = location.getId().toString();
      BoundingBox locationBounds =
          new BoundingBox(
              location.getLatitude(),
              location.getLongitude(),
              location.getLatitude(),
              location.getLongitude());
      MarkerCluster markerCluster =
          new MarkerCluster()
              .id(id)
              .coordinates(
                  new Coordinates()
                      .latitude(location.getLatitude())
                      .longitude(location.getLongitude()))
              .count(1)
              .sampleVideoIds(new ArrayList<>())
              .bounds(locationBounds);

      markerClusters.add(markerCluster);
      totalLocations++;
    }

    ClusterResponse response =
        new ClusterResponse().clusters(markerClusters).totalLocations(totalLocations).zoom(zoom);

    return ResponseEntity.ok(response);
  }

  private double[] parseBoundingBox(String bbox) {
    // bbox is already validated by API spec regex: minLng,minLat,maxLng,maxLat
    String[] parts = bbox.split(",", -1);
    return new double[] {
      Double.parseDouble(parts[0]), // minLng
      Double.parseDouble(parts[1]), // minLat
      Double.parseDouble(parts[2]), // maxLng
      Double.parseDouble(parts[3]) // maxLat
    };
  }

  private String generateClusterId(double lat, double lng, int zoom) {
    // Generate a simple geohash-like ID based on coordinates and zoom
    // This creates a predictable ID for the same cluster
    long latPart = Double.doubleToLongBits(lat);
    long lngPart = Double.doubleToLongBits(lng);
    return String.format("cluster_%d_%x_%x", zoom, latPart, lngPart);
  }
}
