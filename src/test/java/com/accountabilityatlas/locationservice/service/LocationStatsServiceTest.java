package com.accountabilityatlas.locationservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.accountabilityatlas.locationservice.domain.LocationStats;
import com.accountabilityatlas.locationservice.repository.LocationStatsRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocationStatsServiceTest {

  @Mock private LocationStatsRepository locationStatsRepository;

  @InjectMocks private LocationStatsService locationStatsService;

  @Test
  void incrementVideoCount_existingStats_incrementsCount() {
    // Arrange
    UUID locationId = UUID.randomUUID();
    LocationStats stats = new LocationStats();
    stats.setVideoCount(0);
    when(locationStatsRepository.findById(locationId)).thenReturn(Optional.of(stats));

    // Act
    locationStatsService.incrementVideoCount(List.of(locationId));

    // Assert
    assertThat(stats.getVideoCount()).isEqualTo(1);
    verify(locationStatsRepository).save(stats);
  }

  @Test
  void decrementVideoCount_existingStats_decrementsCount() {
    // Arrange
    UUID locationId = UUID.randomUUID();
    LocationStats stats = new LocationStats();
    stats.setVideoCount(3);
    when(locationStatsRepository.findById(locationId)).thenReturn(Optional.of(stats));

    // Act
    locationStatsService.decrementVideoCount(List.of(locationId));

    // Assert
    assertThat(stats.getVideoCount()).isEqualTo(2);
    verify(locationStatsRepository).save(stats);
  }

  @Test
  void decrementVideoCount_zeroCount_doesNotGoBelowZero() {
    // Arrange
    UUID locationId = UUID.randomUUID();
    LocationStats stats = new LocationStats();
    stats.setVideoCount(0);
    when(locationStatsRepository.findById(locationId)).thenReturn(Optional.of(stats));

    // Act
    locationStatsService.decrementVideoCount(List.of(locationId));

    // Assert
    assertThat(stats.getVideoCount()).isZero();
    verify(locationStatsRepository).save(stats);
  }

  @Test
  void incrementVideoCount_nonExistentLocation_skipsWithoutSaving() {
    // Arrange
    UUID locationId = UUID.randomUUID();
    when(locationStatsRepository.findById(locationId)).thenReturn(Optional.empty());

    // Act
    locationStatsService.incrementVideoCount(List.of(locationId));

    // Assert
    verify(locationStatsRepository, never()).save(any());
  }
}
