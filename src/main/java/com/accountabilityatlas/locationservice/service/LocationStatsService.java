package com.accountabilityatlas.locationservice.service;

import com.accountabilityatlas.locationservice.repository.LocationStatsRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationStatsService {

  private final LocationStatsRepository locationStatsRepository;

  @Transactional
  public void incrementVideoCount(List<UUID> locationIds) {
    for (UUID locationId : locationIds) {
      locationStatsRepository
          .findById(locationId)
          .ifPresent(
              stats -> {
                stats.incrementVideoCount();
                locationStatsRepository.save(stats);
                log.debug("Incremented video count for location {}", locationId);
              });
    }
  }

  @Transactional
  public void decrementVideoCount(List<UUID> locationIds) {
    for (UUID locationId : locationIds) {
      locationStatsRepository
          .findById(locationId)
          .ifPresent(
              stats -> {
                stats.decrementVideoCount();
                locationStatsRepository.save(stats);
                log.debug("Decremented video count for location {}", locationId);
              });
    }
  }
}
