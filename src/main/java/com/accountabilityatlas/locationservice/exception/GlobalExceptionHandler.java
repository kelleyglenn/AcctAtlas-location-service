package com.accountabilityatlas.locationservice.exception;

import com.accountabilityatlas.locationservice.web.model.Error;
import com.accountabilityatlas.locationservice.web.model.FieldError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(LocationNotFoundException.class)
  public ResponseEntity<Error> handleLocationNotFound(LocationNotFoundException ex) {
    Error error = new Error();
    error.setCode("LOCATION_NOT_FOUND");
    error.setMessage(ex.getMessage());
    error.setTraceId(generateTraceId());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(GeocodingNotFoundException.class)
  public ResponseEntity<Error> handleGeocodingNotFound(GeocodingNotFoundException ex) {
    Error error = new Error();
    error.setCode("GEOCODING_NOT_FOUND");
    error.setMessage(ex.getMessage());
    error.setTraceId(generateTraceId());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Error> handleValidation(MethodArgumentNotValidException ex) {
    Error error = new Error();
    error.setCode("VALIDATION_ERROR");
    error.setMessage("Request validation failed");
    error.setTraceId(generateTraceId());

    ex.getBindingResult()
        .getFieldErrors()
        .forEach(
            fieldError -> {
              FieldError detail = new FieldError();
              detail.setField(fieldError.getField());
              detail.setMessage(fieldError.getDefaultMessage());
              error.addDetailsItem(detail);
            });

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Error> handleConstraintViolation(ConstraintViolationException ex) {
    Error error = new Error();
    error.setCode("VALIDATION_ERROR");
    error.setMessage("Request validation failed");
    error.setTraceId(generateTraceId());

    for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
      FieldError detail = new FieldError();
      detail.setField(violation.getPropertyPath().toString());
      detail.setMessage(violation.getMessage());
      error.addDetailsItem(detail);
    }

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Error> handleIllegalArgument(IllegalArgumentException ex) {
    Error error = new Error();
    error.setCode("BAD_REQUEST");
    error.setMessage(ex.getMessage());
    error.setTraceId(generateTraceId());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Error> handleGenericException(Exception ex) {
    UUID traceId = generateTraceId();
    log.error("Unhandled exception [traceId={}]", traceId, ex);

    Error error = new Error();
    error.setCode("INTERNAL_SERVER_ERROR");
    error.setMessage("An unexpected error occurred");
    error.setTraceId(traceId);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  private UUID generateTraceId() {
    return UUID.randomUUID();
  }
}
