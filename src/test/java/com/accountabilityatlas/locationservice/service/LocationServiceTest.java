package com.accountabilityatlas.locationservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.accountabilityatlas.locationservice.domain.Location;
import com.accountabilityatlas.locationservice.exception.LocationNotFoundException;
import com.accountabilityatlas.locationservice.repository.LocationRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
class LocationServiceTest {

  private static final GeometryFactory GEOMETRY_FACTORY =
      new GeometryFactory(new PrecisionModel(), 4326);

  @Mock private LocationRepository locationRepository;

  @InjectMocks private LocationService locationService;

  @Test
  void shouldCreateLocation() {
    double lat = 37.7749;
    double lng = -122.4194;
    String displayName = "San Francisco City Hall";
    Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));
    Location expectedLocation =
        Location.builder().coordinates(point).displayName(displayName).build();

    when(locationRepository.save(any(Location.class))).thenReturn(expectedLocation);

    Location result = locationService.createLocation(lat, lng, displayName, null, null, null, null);

    assertThat(result.getDisplayName()).isEqualTo(displayName);
    verify(locationRepository).save(any(Location.class));
  }

  @Test
  void shouldGetLocationById() {
    UUID id = UUID.randomUUID();
    Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(-122.4194, 37.7749));
    Location location = Location.builder().coordinates(point).displayName("Test").build();

    when(locationRepository.findById(id)).thenReturn(Optional.of(location));

    Location result = locationService.getLocation(id);

    assertThat(result.getDisplayName()).isEqualTo("Test");
  }

  @Test
  void shouldThrowWhenLocationNotFound() {
    UUID id = UUID.randomUUID();
    when(locationRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> locationService.getLocation(id))
        .isInstanceOf(LocationNotFoundException.class);
  }

  @Test
  void shouldGetLocationsInBoundingBox() {
    Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(-122.4194, 37.7749));
    Location location = Location.builder().coordinates(point).displayName("SF").build();

    when(locationRepository.findWithinBoundingBox(any())).thenReturn(List.of(location));

    List<Location> results = locationService.getLocationsInBoundingBox(-123.0, 37.0, -122.0, 38.0);

    assertThat(results).hasSize(1);
    assertThat(results.get(0).getDisplayName()).isEqualTo("SF");
  }
}
