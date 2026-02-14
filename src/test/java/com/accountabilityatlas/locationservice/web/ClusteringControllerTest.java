package com.accountabilityatlas.locationservice.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.accountabilityatlas.locationservice.domain.Location;
import com.accountabilityatlas.locationservice.service.ClusteringService;
import com.accountabilityatlas.locationservice.service.ClusteringService.Cluster;
import com.accountabilityatlas.locationservice.service.ClusteringService.ClusterResult;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ClusteringController.class)
class ClusteringControllerTest {

  private static final GeometryFactory GEOMETRY_FACTORY =
      new GeometryFactory(new PrecisionModel(), 4326);

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ClusteringService clusteringService;

  @Test
  void shouldReturnClustersAtLowZoom() throws Exception {
    int zoom = 10;
    double minLng = -123.0;
    double minLat = 37.0;
    double maxLng = -122.0;
    double maxLat = 38.0;

    List<Cluster> clusters =
        List.of(
            new Cluster(37.7749, -122.4194, 5, 12, 37.5, 38.0, -122.5, -122.3),
            new Cluster(37.8044, -122.2712, 3, 12, 37.7, 37.9, -122.4, -122.1));
    ClusterResult result = new ClusterResult(clusters, Collections.emptyList());

    when(clusteringService.getClusters(minLng, minLat, maxLng, maxLat, zoom)).thenReturn(result);

    mockMvc
        .perform(
            get("/locations/cluster").param("bbox", "-123.0,37.0,-122.0,38.0").param("zoom", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalLocations").value(8)) // 5 + 3
        .andExpect(jsonPath("$.zoom").value(10))
        .andExpect(jsonPath("$.clusters.length()").value(2))
        .andExpect(jsonPath("$.clusters[0].count").value(5))
        .andExpect(jsonPath("$.clusters[0].coordinates.latitude").value(37.7749))
        .andExpect(jsonPath("$.clusters[0].coordinates.longitude").value(-122.4194))
        .andExpect(jsonPath("$.clusters[0].bounds.minLat").value(37.5))
        .andExpect(jsonPath("$.clusters[0].bounds.maxLat").value(38.0))
        .andExpect(jsonPath("$.clusters[0].bounds.minLng").value(-122.5))
        .andExpect(jsonPath("$.clusters[0].bounds.maxLng").value(-122.3))
        .andExpect(jsonPath("$.clusters[1].count").value(3))
        .andExpect(jsonPath("$.clusters[1].bounds.minLat").value(37.7))
        .andExpect(jsonPath("$.clusters[1].bounds.maxLat").value(37.9));
  }

  @Test
  void shouldReturnIndividualLocationsAtHighZoom() throws Exception {
    int zoom = 16;
    double minLng = -122.5;
    double minLat = 37.5;
    double maxLng = -122.3;
    double maxLat = 37.9;

    UUID id = UUID.randomUUID();
    Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(-122.4194, 37.7749));
    Location location = Location.builder().coordinates(point).displayName("SF City Hall").build();
    location.setId(id);

    ClusterResult result = new ClusterResult(Collections.emptyList(), List.of(location));

    when(clusteringService.getClusters(minLng, minLat, maxLng, maxLat, zoom)).thenReturn(result);

    mockMvc
        .perform(
            get("/locations/cluster").param("bbox", "-122.5,37.5,-122.3,37.9").param("zoom", "16"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalLocations").value(1))
        .andExpect(jsonPath("$.zoom").value(16))
        .andExpect(jsonPath("$.clusters.length()").value(1))
        .andExpect(jsonPath("$.clusters[0].count").value(1))
        .andExpect(jsonPath("$.clusters[0].id").value(id.toString()))
        .andExpect(jsonPath("$.clusters[0].coordinates.latitude").value(37.7749))
        .andExpect(jsonPath("$.clusters[0].coordinates.longitude").value(-122.4194));
  }

  @Test
  void shouldReturnEmptyClustersForEmptyBoundingBox() throws Exception {
    int zoom = 10;
    double minLng = -100.0;
    double minLat = 50.0;
    double maxLng = -99.0;
    double maxLat = 51.0;

    ClusterResult result = new ClusterResult(Collections.emptyList(), Collections.emptyList());

    when(clusteringService.getClusters(minLng, minLat, maxLng, maxLat, zoom)).thenReturn(result);

    mockMvc
        .perform(
            get("/locations/cluster").param("bbox", "-100.0,50.0,-99.0,51.0").param("zoom", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalLocations").value(0))
        .andExpect(jsonPath("$.zoom").value(10))
        .andExpect(jsonPath("$.clusters.length()").value(0));
  }

  @Test
  void shouldRejectInvalidBbox() throws Exception {
    // Invalid bbox format is caught by OpenAPI validation regex, handled by GlobalExceptionHandler
    mockMvc
        .perform(get("/locations/cluster").param("bbox", "invalid-bbox").param("zoom", "10"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.traceId").exists());
  }

  @Test
  void shouldRejectZoomOutOfRange() throws Exception {
    // Zoom must be between 1 and 20
    // When zoom=0 is provided, the controller should return validation error
    mockMvc
        .perform(
            get("/locations/cluster").param("bbox", "-123.0,37.0,-122.0,38.0").param("zoom", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.traceId").exists());

    // When zoom=21 is provided, the controller should return validation error
    mockMvc
        .perform(
            get("/locations/cluster").param("bbox", "-123.0,37.0,-122.0,38.0").param("zoom", "21"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.traceId").exists());
  }

  @Test
  void shouldAcceptGridSizeParameter() throws Exception {
    int zoom = 10;
    double minLng = -123.0;
    double minLat = 37.0;
    double maxLng = -122.0;
    double maxLat = 38.0;

    List<Cluster> clusters =
        List.of(new Cluster(37.7749, -122.4194, 5, 12, 37.5, 38.0, -122.5, -122.3));
    ClusterResult result = new ClusterResult(clusters, Collections.emptyList());

    when(clusteringService.getClusters(minLng, minLat, maxLng, maxLat, zoom)).thenReturn(result);

    mockMvc
        .perform(
            get("/locations/cluster")
                .param("bbox", "-123.0,37.0,-122.0,38.0")
                .param("zoom", "10")
                .param("gridSize", "80"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalLocations").value(5));
  }
}
