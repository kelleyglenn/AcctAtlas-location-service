package com.accountabilityatlas.locationservice.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.accountabilityatlas.locationservice.service.LocationStatsService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VideoStatusChangedHandlerTest {

  @Mock private LocationStatsService locationStatsService;
  @InjectMocks private VideoStatusChangedHandler handler;

  @Test
  void handleVideoStatusChanged_toApproved_incrementsVideoCount() {
    // Arrange
    UUID locationId = UUID.randomUUID();
    VideoStatusChangedEvent event =
        new VideoStatusChangedEvent(
            UUID.randomUUID(), List.of(locationId), "PENDING", "APPROVED", Instant.now());

    // Act
    handler.handleVideoStatusChanged(event);

    // Assert
    verify(locationStatsService).incrementVideoCount(List.of(locationId));
    verify(locationStatsService, never()).decrementVideoCount(any());
  }

  @Test
  void handleVideoStatusChanged_fromApproved_decrementsVideoCount() {
    // Arrange
    UUID locationId = UUID.randomUUID();
    VideoStatusChangedEvent event =
        new VideoStatusChangedEvent(
            UUID.randomUUID(), List.of(locationId), "APPROVED", "REJECTED", Instant.now());

    // Act
    handler.handleVideoStatusChanged(event);

    // Assert
    verify(locationStatsService).decrementVideoCount(List.of(locationId));
    verify(locationStatsService, never()).incrementVideoCount(any());
  }

  @Test
  void handleVideoStatusChanged_nonApprovedTransition_doesNothing() {
    // Arrange
    VideoStatusChangedEvent event =
        new VideoStatusChangedEvent(
            UUID.randomUUID(), List.of(UUID.randomUUID()), "PENDING", "REJECTED", Instant.now());

    // Act
    handler.handleVideoStatusChanged(event);

    // Assert
    verifyNoInteractions(locationStatsService);
  }

  @Test
  void handleVideoStatusChanged_multipleLocations_passesAllIds() {
    // Arrange
    UUID loc1 = UUID.randomUUID();
    UUID loc2 = UUID.randomUUID();
    UUID loc3 = UUID.randomUUID();
    VideoStatusChangedEvent event =
        new VideoStatusChangedEvent(
            UUID.randomUUID(), List.of(loc1, loc2, loc3), "PENDING", "APPROVED", Instant.now());

    // Act
    handler.handleVideoStatusChanged(event);

    // Assert
    verify(locationStatsService).incrementVideoCount(List.of(loc1, loc2, loc3));
  }

  @Test
  void handleVideoStatusChanged_serviceFailure_rethrowsException() {
    // Arrange
    UUID locationId = UUID.randomUUID();
    VideoStatusChangedEvent event =
        new VideoStatusChangedEvent(
            UUID.randomUUID(), List.of(locationId), "PENDING", "APPROVED", Instant.now());

    RuntimeException dbException = new RuntimeException("Database unavailable");
    doThrow(dbException).when(locationStatsService).incrementVideoCount(List.of(locationId));

    // Act & Assert
    assertThatThrownBy(() -> handler.handleVideoStatusChanged(event)).isSameAs(dbException);
  }
}
