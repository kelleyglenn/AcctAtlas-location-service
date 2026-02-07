package com.accountabilityatlas.locationservice.exception;

public class GeocodingNotFoundException extends RuntimeException {

  public GeocodingNotFoundException(String query) {
    super("No results found for: " + query);
  }

  public GeocodingNotFoundException(double latitude, double longitude) {
    super("No address found for coordinates: " + latitude + ", " + longitude);
  }
}
