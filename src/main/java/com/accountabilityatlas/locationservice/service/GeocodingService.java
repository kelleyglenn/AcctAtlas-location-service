package com.accountabilityatlas.locationservice.service;

import com.accountabilityatlas.locationservice.config.MapboxProperties;
import com.accountabilityatlas.locationservice.exception.GeocodingNotFoundException;
import com.accountabilityatlas.locationservice.web.model.Coordinates;
import com.accountabilityatlas.locationservice.web.model.GeocodeResponse;
import com.accountabilityatlas.locationservice.web.model.ReverseGeocodeResponse;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class GeocodingService {

  private static final String MAPBOX_BASE_URL = "https://api.mapbox.com/geocoding/v5/mapbox.places";

  private final RestClient restClient;
  private final MapboxProperties mapboxProperties;

  @Autowired
  public GeocodingService(MapboxProperties mapboxProperties) {
    this.mapboxProperties = mapboxProperties;
    this.restClient = RestClient.builder().baseUrl(MAPBOX_BASE_URL).build();
  }

  // Constructor for testing with custom RestClient
  GeocodingService(RestClient restClient, MapboxProperties mapboxProperties) {
    this.restClient = restClient;
    this.mapboxProperties = mapboxProperties;
  }

  public GeocodeResponse geocode(String address) {
    log.debug("Geocoding address: {}", address);
    String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
    String uri =
        String.format("/%s.json?access_token=%s", encodedAddress, mapboxProperties.accessToken());

    JsonNode response = restClient.get().uri(uri).retrieve().body(JsonNode.class);

    return parseGeocodeResponse(response, address);
  }

  public ReverseGeocodeResponse reverseGeocode(double latitude, double longitude) {
    log.debug("Reverse geocoding coordinates: {}, {}", latitude, longitude);
    String uri =
        String.format(
            Locale.US,
            "/%f,%f.json?access_token=%s",
            longitude,
            latitude,
            mapboxProperties.accessToken());

    JsonNode response = restClient.get().uri(uri).retrieve().body(JsonNode.class);

    return parseReverseGeocodeResponse(response, latitude, longitude);
  }

  private GeocodeResponse parseGeocodeResponse(JsonNode response, String query) {
    JsonNode features = response.path("features");
    if (features.isEmpty()) {
      throw new GeocodingNotFoundException(query);
    }

    JsonNode feature = features.get(0);
    JsonNode center = feature.path("center");

    double longitude = center.get(0).asDouble();
    double latitude = center.get(1).asDouble();
    String placeName = feature.path("place_name").asText();

    String city = extractContextValue(feature, "place");
    String state = extractContextValue(feature, "region");
    String country = extractContextValue(feature, "country");
    String placeId = feature.path("id").asText(null);

    return new GeocodeResponse()
        .coordinates(new Coordinates(latitude, longitude))
        .formattedAddress(placeName)
        .city(city)
        .state(state)
        .country(country)
        .placeId(placeId);
  }

  private ReverseGeocodeResponse parseReverseGeocodeResponse(
      JsonNode response, double latitude, double longitude) {
    JsonNode features = response.path("features");
    if (features.isEmpty()) {
      throw new GeocodingNotFoundException(latitude, longitude);
    }

    JsonNode feature = features.get(0);
    String placeName = feature.path("place_name").asText();
    String streetAddress = feature.path("text").asText(null);

    String city = extractContextValue(feature, "place");
    String state = extractContextValue(feature, "region");
    String country = extractContextValue(feature, "country");
    String placeId = feature.path("id").asText(null);

    return new ReverseGeocodeResponse()
        .formattedAddress(placeName)
        .address(streetAddress)
        .city(city)
        .state(state)
        .country(country)
        .placeId(placeId);
  }

  private String extractContextValue(JsonNode feature, String contextType) {
    JsonNode context = feature.path("context");
    if (context.isMissingNode() || !context.isArray()) {
      return null;
    }

    for (JsonNode contextItem : context) {
      String id = contextItem.path("id").asText("");
      if (id.startsWith(contextType + ".")) {
        return contextItem.path("text").asText(null);
      }
    }
    return null;
  }
}
