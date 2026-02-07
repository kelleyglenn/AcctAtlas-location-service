package com.accountabilityatlas.locationservice.web;

import com.accountabilityatlas.locationservice.service.GeocodingService;
import com.accountabilityatlas.locationservice.web.api.GeocodingApi;
import com.accountabilityatlas.locationservice.web.model.GeocodeResponse;
import com.accountabilityatlas.locationservice.web.model.ReverseGeocodeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GeocodingController implements GeocodingApi {

  private final GeocodingService geocodingService;

  @Override
  public ResponseEntity<GeocodeResponse> geocodeAddress(String address) {
    GeocodeResponse response = geocodingService.geocode(address);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<ReverseGeocodeResponse> reverseGeocode(Double latitude, Double longitude) {
    ReverseGeocodeResponse response = geocodingService.reverseGeocode(latitude, longitude);
    return ResponseEntity.ok(response);
  }
}
