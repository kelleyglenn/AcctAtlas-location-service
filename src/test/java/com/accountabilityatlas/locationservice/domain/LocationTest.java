package com.accountabilityatlas.locationservice.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

class LocationTest {

  private static final GeometryFactory GEOMETRY_FACTORY =
      new GeometryFactory(new PrecisionModel(), 4326);

  @Test
  void shouldCreateLocationWithValidCoordinates() {
    Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(-122.4194, 37.7749));

    Location location =
        Location.builder()
            .coordinates(point)
            .displayName("San Francisco City Hall")
            .city("San Francisco")
            .state("CA")
            .country("USA")
            .build();

    assertThat(location.getDisplayName()).isEqualTo("San Francisco City Hall");
    assertThat(location.getCoordinates().getX()).isEqualTo(-122.4194);
    assertThat(location.getCoordinates().getY()).isEqualTo(37.7749);
  }

  @Test
  void shouldProvideLatitudeLongitudeHelpers() {
    Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(-122.4194, 37.7749));
    Location location = Location.builder().coordinates(point).displayName("Test").build();

    assertThat(location.getLatitude()).isEqualTo(37.7749);
    assertThat(location.getLongitude()).isEqualTo(-122.4194);
  }
}
