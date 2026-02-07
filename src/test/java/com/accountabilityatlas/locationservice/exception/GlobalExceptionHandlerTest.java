package com.accountabilityatlas.locationservice.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.accountabilityatlas.locationservice.web.model.Error;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
  }

  @Test
  void shouldHandleLocationNotFoundException() {
    UUID id = UUID.randomUUID();
    LocationNotFoundException ex = new LocationNotFoundException(id);

    ResponseEntity<Error> response = handler.handleLocationNotFound(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("LOCATION_NOT_FOUND");
    assertThat(response.getBody().getMessage()).isEqualTo("Location not found: " + id);
    assertThat(response.getBody().getTraceId()).isNotNull();
  }

  @Test
  void shouldHandleGeocodingNotFoundException() {
    GeocodingNotFoundException ex = new GeocodingNotFoundException("123 Main St");

    ResponseEntity<Error> response = handler.handleGeocodingNotFound(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("GEOCODING_NOT_FOUND");
    assertThat(response.getBody().getMessage()).isEqualTo("No results found for: 123 Main St");
    assertThat(response.getBody().getTraceId()).isNotNull();
  }

  @Test
  void shouldHandleGeocodingNotFoundExceptionWithCoordinates() {
    GeocodingNotFoundException ex = new GeocodingNotFoundException(37.7749, -122.4194);

    ResponseEntity<Error> response = handler.handleGeocodingNotFound(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("GEOCODING_NOT_FOUND");
    assertThat(response.getBody().getMessage())
        .isEqualTo("No address found for coordinates: 37.7749, -122.4194");
    assertThat(response.getBody().getTraceId()).isNotNull();
  }

  @Test
  void shouldHandleIllegalArgumentException() {
    IllegalArgumentException ex = new IllegalArgumentException("Invalid bounding box format");

    ResponseEntity<Error> response = handler.handleIllegalArgument(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("BAD_REQUEST");
    assertThat(response.getBody().getMessage()).isEqualTo("Invalid bounding box format");
    assertThat(response.getBody().getTraceId()).isNotNull();
  }

  @Test
  void shouldHandleMethodArgumentNotValidException() {
    BindingResult bindingResult = Mockito.mock(BindingResult.class);
    FieldError fieldError1 = new FieldError("request", "latitude", "must not be null");
    FieldError fieldError2 = new FieldError("request", "longitude", "must be between -180 and 180");

    Mockito.when(bindingResult.getFieldErrors())
        .thenReturn(java.util.List.of(fieldError1, fieldError2));

    MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

    ResponseEntity<Error> response = handler.handleValidation(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
    assertThat(response.getBody().getMessage()).isEqualTo("Request validation failed");
    assertThat(response.getBody().getDetails()).hasSize(2);
    assertThat(response.getBody().getDetails().get(0).getField()).isEqualTo("latitude");
    assertThat(response.getBody().getDetails().get(0).getMessage()).isEqualTo("must not be null");
    assertThat(response.getBody().getDetails().get(1).getField()).isEqualTo("longitude");
    assertThat(response.getBody().getTraceId()).isNotNull();
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldHandleConstraintViolationException() {
    ConstraintViolation<Object> violation = Mockito.mock(ConstraintViolation.class);
    Path path = Mockito.mock(Path.class);
    Mockito.when(path.toString()).thenReturn("zoom");
    Mockito.when(violation.getPropertyPath()).thenReturn(path);
    Mockito.when(violation.getMessage()).thenReturn("must be between 1 and 20");

    ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

    ResponseEntity<Error> response = handler.handleConstraintViolation(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
    assertThat(response.getBody().getMessage()).isEqualTo("Request validation failed");
    assertThat(response.getBody().getDetails()).hasSize(1);
    assertThat(response.getBody().getDetails().get(0).getField()).isEqualTo("zoom");
    assertThat(response.getBody().getDetails().get(0).getMessage())
        .isEqualTo("must be between 1 and 20");
    assertThat(response.getBody().getTraceId()).isNotNull();
  }

  @Test
  void shouldHandleGenericException() {
    Exception ex = new RuntimeException("Something went wrong");

    ResponseEntity<Error> response = handler.handleGenericException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
    assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    assertThat(response.getBody().getTraceId()).isNotNull();
  }

  @Test
  void shouldGenerateUniqueTraceIds() {
    UUID id = UUID.randomUUID();
    LocationNotFoundException ex = new LocationNotFoundException(id);

    ResponseEntity<Error> response1 = handler.handleLocationNotFound(ex);
    ResponseEntity<Error> response2 = handler.handleLocationNotFound(ex);

    assertThat(response1.getBody().getTraceId()).isNotEqualTo(response2.getBody().getTraceId());
  }
}
