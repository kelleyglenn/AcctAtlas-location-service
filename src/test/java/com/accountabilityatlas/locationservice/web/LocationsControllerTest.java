package com.accountabilityatlas.locationservice.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.accountabilityatlas.locationservice.domain.Location;
import com.accountabilityatlas.locationservice.service.LocationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LocationsController.class)
class LocationsControllerTest {

  private static final GeometryFactory GEOMETRY_FACTORY =
      new GeometryFactory(new PrecisionModel(), 4326);

  @Autowired private MockMvc mockMvc;

  @MockitoBean private LocationService locationService;

  @Test
  void shouldCreateLocation() throws Exception {
    UUID id = UUID.randomUUID();
    Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(-122.4194, 37.7749));
    Location location =
        Location.builder()
            .coordinates(point)
            .displayName("San Francisco City Hall")
            .city("San Francisco")
            .state("CA")
            .country("USA")
            .build();
    // Use reflection or setter to set id and createdAt since they're normally set by JPA
    location.setId(id);
    location.setCreatedAt(Instant.now());

    when(locationService.createLocation(
            eq(37.7749), eq(-122.4194), eq("San Francisco City Hall"), any(), any(), any(), any()))
        .thenReturn(location);

    mockMvc
        .perform(
            post("/locations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "coordinates": {
                        "latitude": 37.7749,
                        "longitude": -122.4194
                      },
                      "displayName": "San Francisco City Hall",
                      "city": "San Francisco",
                      "state": "CA",
                      "country": "USA"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.displayName").value("San Francisco City Hall"))
        .andExpect(jsonPath("$.city").value("San Francisco"))
        .andExpect(jsonPath("$.state").value("CA"))
        .andExpect(jsonPath("$.country").value("USA"))
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.coordinates.latitude").value(37.7749))
        .andExpect(jsonPath("$.coordinates.longitude").value(-122.4194));
  }

  @Test
  void shouldGetLocationById() throws Exception {
    UUID id = UUID.randomUUID();
    Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(-122.4194, 37.7749));
    Location location =
        Location.builder()
            .coordinates(point)
            .displayName("San Francisco City Hall")
            .city("San Francisco")
            .state("CA")
            .country("USA")
            .build();
    location.setId(id);
    location.setCreatedAt(Instant.now());

    when(locationService.getLocation(id)).thenReturn(location);

    mockMvc
        .perform(get("/locations/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("San Francisco City Hall"))
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.coordinates.latitude").value(37.7749))
        .andExpect(jsonPath("$.coordinates.longitude").value(-122.4194));
  }

  @Test
  void shouldListLocationsWithinBoundingBox() throws Exception {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    Point point1 = GEOMETRY_FACTORY.createPoint(new Coordinate(-122.4194, 37.7749));
    Point point2 = GEOMETRY_FACTORY.createPoint(new Coordinate(-122.4, 37.78));

    Location location1 = Location.builder().coordinates(point1).displayName("Location 1").build();
    location1.setId(id1);
    location1.setCreatedAt(Instant.now());

    Location location2 = Location.builder().coordinates(point2).displayName("Location 2").build();
    location2.setId(id2);
    location2.setCreatedAt(Instant.now());

    when(locationService.getLocationsInBoundingBox(-123.0, 37.0, -122.0, 38.0))
        .thenReturn(List.of(location1, location2));

    mockMvc
        .perform(get("/locations").param("bbox", "-123.0,37.0,-122.0,38.0"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count").value(2))
        .andExpect(jsonPath("$.locations[0].displayName").value("Location 1"))
        .andExpect(jsonPath("$.locations[1].displayName").value("Location 2"))
        .andExpect(jsonPath("$.truncated").value(false));
  }

  @Test
  void shouldTruncateLocationsWhenExceedingLimit() throws Exception {
    UUID id = UUID.randomUUID();
    Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(-122.4194, 37.7749));
    Location location = Location.builder().coordinates(point).displayName("Only Location").build();
    location.setId(id);
    location.setCreatedAt(Instant.now());

    // Return more locations than the limit
    List<Location> manyLocations = List.of(location, location, location);

    when(locationService.getLocationsInBoundingBox(-123.0, 37.0, -122.0, 38.0))
        .thenReturn(manyLocations);

    mockMvc
        .perform(get("/locations").param("bbox", "-123.0,37.0,-122.0,38.0").param("limit", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count").value(2))
        .andExpect(jsonPath("$.truncated").value(true));
  }
}
