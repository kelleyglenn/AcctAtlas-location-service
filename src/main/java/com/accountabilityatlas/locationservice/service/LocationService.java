package com.accountabilityatlas.locationservice.service;

import com.accountabilityatlas.locationservice.domain.Location;
import com.accountabilityatlas.locationservice.exception.LocationNotFoundException;
import com.accountabilityatlas.locationservice.repository.LocationRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LocationService {

  private static final GeometryFactory GEOMETRY_FACTORY =
      new GeometryFactory(new PrecisionModel(), 4326);

  private final LocationRepository locationRepository;

  @Transactional
  public Location createLocation(
      double latitude,
      double longitude,
      String displayName,
      String address,
      String city,
      String state,
      String country) {
    Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    Location location =
        Location.builder()
            .coordinates(point)
            .displayName(displayName)
            .address(address)
            .city(city)
            .state(state)
            .country(country)
            .build();
    return locationRepository.save(location);
  }

  @Transactional(readOnly = true)
  public Location getLocation(UUID id) {
    return locationRepository.findById(id).orElseThrow(() -> new LocationNotFoundException(id));
  }

  @Transactional(readOnly = true)
  public List<Location> getLocationsInBoundingBox(
      double minLng, double minLat, double maxLng, double maxLat) {
    Polygon bbox =
        GEOMETRY_FACTORY.createPolygon(
            new Coordinate[] {
              new Coordinate(minLng, minLat),
              new Coordinate(maxLng, minLat),
              new Coordinate(maxLng, maxLat),
              new Coordinate(minLng, maxLat),
              new Coordinate(minLng, minLat)
            });
    return locationRepository.findWithinBoundingBox(bbox);
  }
}
