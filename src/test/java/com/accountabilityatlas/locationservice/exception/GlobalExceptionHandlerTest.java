package com.accountabilityatlas.locationservice.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
  }

  @Test
  void handleLocationNotFound_validException_returns404WithCode() {
    // Arrange
    UUID id = UUID.randomUUID();
    LocationNotFoundException ex = new LocationNotFoundException(id);

    // Act
    ResponseEntity<Error> response = handler.handleLocationNotFound(ex);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("LOCATION_NOT_FOUND");
    assertThat(response.getBody().getMessage()).isEqualTo("Location not found: " + id);
    assertThat(response.getBody().getTraceId()).isNotNull();
  }

  @Test
  void handleGeocodingNotFound_addressQuery_returns404WithMessage() {
    // Arrange
    GeocodingNotFoundException ex = new GeocodingNotFoundException("123 Main St");

    // Act
    ResponseEntity<Error> response = handler.handleGeocodingNotFound(ex);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("GEOCODING_NOT_FOUND");
    assertThat(response.getBody().getMessage()).isEqualTo("No results found for: 123 Main St");
    assertThat(response.getBody().getTraceId()).isNotNull();
  }

  @Test
  void handleGeocodingNotFound_coordinateQuery_returns404WithCoordinates() {
    // Arrange
    GeocodingNotFoundException ex = new GeocodingNotFoundException(37.7749, -122.4194);

    // Act
    ResponseEntity<Error> response = handler.handleGeocodingNotFound(ex);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("GEOCODING_NOT_FOUND");
    assertThat(response.getBody().getMessage())
        .isEqualTo("No address found for coordinates: 37.7749, -122.4194");
    assertThat(response.getBody().getTraceId()).isNotNull();
  }

  @Test
  void handleIllegalArgument_invalidInput_returns400() {
    // Arrange
    IllegalArgumentException ex = new IllegalArgumentException("Invalid bounding box format");

    // Act
    ResponseEntity<Error> response = handler.handleIllegalArgument(ex);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("BAD_REQUEST");
    assertThat(response.getBody().getMessage()).isEqualTo("Invalid bounding box format");
    assertThat(response.getBody().getTraceId()).isNotNull();
  }

  @Test
  void handleValidation_multipleFieldErrors_returns400WithDetails() {
    // Arrange
    BindingResult bindingResult = Mockito.mock(BindingResult.class);
    FieldError fieldError1 = new FieldError("request", "latitude", "must not be null");
    FieldError fieldError2 = new FieldError("request", "longitude", "must be between -180 and 180");

    Mockito.when(bindingResult.getFieldErrors())
        .thenReturn(java.util.List.of(fieldError1, fieldError2));

    MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

    // Act
    ResponseEntity<Error> response = handler.handleValidation(ex);

    // Assert
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
  void handleConstraintViolation_singleViolation_returns400WithDetails() {
    // Arrange
    ConstraintViolation<Object> violation = Mockito.mock(ConstraintViolation.class);
    Path path = Mockito.mock(Path.class);
    Mockito.when(path.toString()).thenReturn("zoom");
    Mockito.when(violation.getPropertyPath()).thenReturn(path);
    Mockito.when(violation.getMessage()).thenReturn("must be between 1 and 20");

    ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

    // Act
    ResponseEntity<Error> response = handler.handleConstraintViolation(ex);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
    assertThat(response.getBody().getMessage()).isEqualTo("Request validation failed");
    assertThat(response.getBody().getDetails()).hasSize(1);
    assertThat(response.getBody().getDetails().getFirst().getField()).isEqualTo("zoom");
    assertThat(response.getBody().getDetails().getFirst().getMessage())
        .isEqualTo("must be between 1 and 20");
    assertThat(response.getBody().getTraceId()).isNotNull();
  }

  @Test
  void handleGenericException_unexpectedError_returns500() {
    // Arrange
    Exception ex = new RuntimeException("Something went wrong");

    // Act
    ResponseEntity<Error> response = handler.handleGenericException(ex);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
    assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    assertThat(response.getBody().getTraceId()).isNotNull();
  }

  @Test
  void handleUnauthorized_missingAuth_returns401() {
    // Arrange
    UnauthorizedException ex = new UnauthorizedException("Authentication required");

    // Act
    ResponseEntity<Error> response = handler.handleUnauthorized(ex);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("UNAUTHORIZED");
    assertThat(response.getBody().getMessage()).isEqualTo("Authentication required");
    assertThat(response.getBody().getTraceId()).isNotNull();
  }

  @Test
  void handleAccessDenied_insufficientPermissions_returns403() {
    // Arrange
    AccessDeniedException ex = new AccessDeniedException("Access denied");

    // Act
    ResponseEntity<Error> response = handler.handleAccessDenied(ex);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("FORBIDDEN");
    assertThat(response.getBody().getMessage()).isEqualTo("Access denied");
    assertThat(response.getBody().getTraceId()).isNotNull();
  }

  @Test
  void handleMissingParameter_missingZoom_returns400WithParameterName() {
    // Arrange
    MissingServletRequestParameterException ex =
        new MissingServletRequestParameterException("zoom", "Integer");

    // Act
    ResponseEntity<Error> response = handler.handleMissingParameter(ex);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
    assertThat(response.getBody().getMessage()).isEqualTo("Required parameter 'zoom' is missing");
    assertThat(response.getBody().getTraceId()).isNotNull();
  }

  @Test
  void handleLocationNotFound_calledTwice_generatesUniqueTraceIds() {
    // Arrange
    UUID id = UUID.randomUUID();
    LocationNotFoundException ex = new LocationNotFoundException(id);

    // Act
    ResponseEntity<Error> response1 = handler.handleLocationNotFound(ex);
    ResponseEntity<Error> response2 = handler.handleLocationNotFound(ex);

    // Assert
    assertNotNull(response1.getBody());
    assertNotNull(response2.getBody());
    assertThat(response1.getBody().getTraceId()).isNotEqualTo(response2.getBody().getTraceId());
  }
}
