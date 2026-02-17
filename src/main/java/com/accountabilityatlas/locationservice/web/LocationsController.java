package com.accountabilityatlas.locationservice.web;

import com.accountabilityatlas.locationservice.domain.Location;
import com.accountabilityatlas.locationservice.exception.UnauthorizedException;
import com.accountabilityatlas.locationservice.service.LocationService;
import com.accountabilityatlas.locationservice.web.api.LocationsApi;
import com.accountabilityatlas.locationservice.web.model.Coordinates;
import com.accountabilityatlas.locationservice.web.model.CreateLocationRequest;
import com.accountabilityatlas.locationservice.web.model.LocationDetail;
import com.accountabilityatlas.locationservice.web.model.LocationListResponse;
import com.accountabilityatlas.locationservice.web.model.LocationSummary;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LocationsController implements LocationsApi {

  private final LocationService locationService;

  @Override
  public ResponseEntity<com.accountabilityatlas.locationservice.web.model.Location> createLocation(
      CreateLocationRequest request) {
    requireAuthentication();
    Location location =
        locationService.createLocation(
            request.getCoordinates().getLatitude(),
            request.getCoordinates().getLongitude(),
            request.getDisplayName(),
            request.getAddress(),
            request.getCity(),
            request.getState(),
            request.getCountry());
    return ResponseEntity.status(HttpStatus.CREATED).body(toLocationDto(location));
  }

  @Override
  public ResponseEntity<LocationDetail> getLocation(UUID id) {
    Location location = locationService.getLocation(id);
    return ResponseEntity.ok(toLocationDetailDto(location));
  }

  @Override
  public ResponseEntity<LocationListResponse> listLocations(
      String bbox, @Nullable List<String> amendments, Integer limit) {
    double[] coords = parseBoundingBox(bbox);
    List<Location> locations =
        locationService.getLocationsInBoundingBox(coords[0], coords[1], coords[2], coords[3]);

    List<Location> limited = locations.stream().limit(limit).toList();
    boolean truncated = locations.size() > limit;

    List<LocationSummary> summaries = limited.stream().map(this::toLocationSummaryDto).toList();

    LocationListResponse response =
        new LocationListResponse()
            .locations(summaries)
            .count(summaries.size())
            .truncated(truncated);

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

  private com.accountabilityatlas.locationservice.web.model.Location toLocationDto(
      Location location) {
    return new com.accountabilityatlas.locationservice.web.model.Location()
        .id(location.getId())
        .coordinates(
            new Coordinates().latitude(location.getLatitude()).longitude(location.getLongitude()))
        .displayName(location.getDisplayName())
        .address(location.getAddress())
        .city(location.getCity())
        .state(location.getState())
        .country(location.getCountry())
        .createdAt(toOffsetDateTime(location));
  }

  private LocationDetail toLocationDetailDto(Location location) {
    Integer videoCount = null;
    if (location.getStats() != null) {
      videoCount = location.getStats().getVideoCount();
    }

    return new LocationDetail()
        .id(location.getId())
        .coordinates(
            new Coordinates().latitude(location.getLatitude()).longitude(location.getLongitude()))
        .displayName(location.getDisplayName())
        .address(location.getAddress())
        .city(location.getCity())
        .state(location.getState())
        .country(location.getCountry())
        .createdAt(toOffsetDateTime(location))
        .videoCount(videoCount);
  }

  private LocationSummary toLocationSummaryDto(Location location) {
    Integer videoCount = null;
    if (location.getStats() != null) {
      videoCount = location.getStats().getVideoCount();
    }

    return new LocationSummary()
        .id(location.getId())
        .coordinates(
            new Coordinates().latitude(location.getLatitude()).longitude(location.getLongitude()))
        .displayName(location.getDisplayName())
        .city(location.getCity())
        .state(location.getState())
        .videoCount(videoCount);
  }

  private OffsetDateTime toOffsetDateTime(Location location) {
    if (location.getCreatedAt() == null) {
      return OffsetDateTime.now(ZoneOffset.UTC);
    }
    return location.getCreatedAt().atOffset(ZoneOffset.UTC);
  }

  private void requireAuthentication() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
      throw new UnauthorizedException("Authentication required");
    }
  }
}
