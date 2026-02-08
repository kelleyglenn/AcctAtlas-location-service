package com.accountabilityatlas.locationservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.accountabilityatlas.locationservice.config.MapboxProperties;
import com.accountabilityatlas.locationservice.exception.GeocodingNotFoundException;
import com.accountabilityatlas.locationservice.web.model.GeocodeResponse;
import com.accountabilityatlas.locationservice.web.model.ReverseGeocodeResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class GeocodingServiceTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String TEST_ACCESS_TOKEN = "test-token";

  private RestClient restClient;
  private RestClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;
  private RestClient.ResponseSpec responseSpec;
  private GeocodingService geocodingService;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    restClient = mock(RestClient.class);
    requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
    responseSpec = mock(RestClient.ResponseSpec.class);

    doReturn(requestHeadersUriSpec).when(restClient).get();
    doReturn(requestHeadersUriSpec).when(requestHeadersUriSpec).uri(anyString());
    when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);

    MapboxProperties properties = new MapboxProperties(TEST_ACCESS_TOKEN);
    geocodingService = new GeocodingService(restClient, properties);
  }

  @Test
  void geocode_withValidAddress_returnsGeocodeResponse() throws Exception {
    String mapboxResponse =
        """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "id": "address.123",
              "type": "Feature",
              "place_type": ["address"],
              "text": "Pennsylvania Avenue Northwest",
              "place_name": "1600 Pennsylvania Avenue NW, Washington, DC 20500, United States",
              "center": [-77.0365, 38.8977],
              "context": [
                {"id": "postcode.123", "text": "20500"},
                {"id": "place.456", "text": "Washington"},
                {"id": "region.789", "text": "District of Columbia"},
                {"id": "country.012", "text": "United States"}
              ]
            }
          ]
        }
        """;

    JsonNode responseNode = OBJECT_MAPPER.readTree(mapboxResponse);
    when(responseSpec.body(JsonNode.class)).thenReturn(responseNode);

    GeocodeResponse result = geocodingService.geocode("1600 Pennsylvania Ave, Washington DC");

    assertThat(result.getCoordinates().getLatitude()).isEqualTo(38.8977);
    assertThat(result.getCoordinates().getLongitude()).isEqualTo(-77.0365);
    assertThat(result.getFormattedAddress())
        .isEqualTo("1600 Pennsylvania Avenue NW, Washington, DC 20500, United States");
    assertThat(result.getCity()).isEqualTo("Washington");
    assertThat(result.getState()).isEqualTo("District of Columbia");
    assertThat(result.getCountry()).isEqualTo("United States");
    assertThat(result.getPlaceId()).isEqualTo("address.123");
  }

  @Test
  void geocode_withNoResults_throwsGeocodingNotFoundException() throws Exception {
    String emptyResponse =
        """
        {
          "type": "FeatureCollection",
          "features": []
        }
        """;

    JsonNode responseNode = OBJECT_MAPPER.readTree(emptyResponse);
    when(responseSpec.body(JsonNode.class)).thenReturn(responseNode);

    assertThatThrownBy(() -> geocodingService.geocode("nonexistent address xyz123"))
        .isInstanceOf(GeocodingNotFoundException.class)
        .hasMessageContaining("nonexistent address xyz123");
  }

  @Test
  void reverseGeocode_withValidCoordinates_returnsReverseGeocodeResponse() throws Exception {
    String mapboxResponse =
        """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "id": "address.456",
              "type": "Feature",
              "place_type": ["address"],
              "text": "Market Street",
              "place_name": "1 Market Street, San Francisco, California 94105, United States",
              "center": [-122.395, 37.791],
              "context": [
                {"id": "postcode.789", "text": "94105"},
                {"id": "place.101", "text": "San Francisco"},
                {"id": "region.102", "text": "California"},
                {"id": "country.103", "text": "United States"}
              ]
            }
          ]
        }
        """;

    JsonNode responseNode = OBJECT_MAPPER.readTree(mapboxResponse);
    when(responseSpec.body(JsonNode.class)).thenReturn(responseNode);

    ReverseGeocodeResponse result = geocodingService.reverseGeocode(37.791, -122.395);

    assertThat(result.getFormattedAddress())
        .isEqualTo("1 Market Street, San Francisco, California 94105, United States");
    assertThat(result.getAddress()).isEqualTo("Market Street");
    assertThat(result.getCity()).isEqualTo("San Francisco");
    assertThat(result.getState()).isEqualTo("California");
    assertThat(result.getCountry()).isEqualTo("United States");
    assertThat(result.getPlaceId()).isEqualTo("address.456");
  }

  @Test
  void reverseGeocode_withNoResults_throwsGeocodingNotFoundException() throws Exception {
    String emptyResponse =
        """
        {
          "type": "FeatureCollection",
          "features": []
        }
        """;

    JsonNode responseNode = OBJECT_MAPPER.readTree(emptyResponse);
    when(responseSpec.body(JsonNode.class)).thenReturn(responseNode);

    assertThatThrownBy(() -> geocodingService.reverseGeocode(0.0, 0.0))
        .isInstanceOf(GeocodingNotFoundException.class)
        .hasMessageContaining("0.0, 0.0");
  }

  @Test
  void geocode_withMissingContext_returnsNullForOptionalFields() throws Exception {
    String responseWithoutContext =
        """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "id": "poi.123",
              "type": "Feature",
              "place_type": ["poi"],
              "text": "Central Park",
              "place_name": "Central Park",
              "center": [-73.9654, 40.7829]
            }
          ]
        }
        """;

    JsonNode responseNode = OBJECT_MAPPER.readTree(responseWithoutContext);
    when(responseSpec.body(JsonNode.class)).thenReturn(responseNode);

    GeocodeResponse result = geocodingService.geocode("Central Park");

    assertThat(result.getCoordinates().getLatitude()).isEqualTo(40.7829);
    assertThat(result.getCoordinates().getLongitude()).isEqualTo(-73.9654);
    assertThat(result.getFormattedAddress()).isEqualTo("Central Park");
    assertThat(result.getCity()).isNull();
    assertThat(result.getState()).isNull();
    assertThat(result.getCountry()).isNull();
  }
}
