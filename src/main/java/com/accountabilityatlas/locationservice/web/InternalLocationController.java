package com.accountabilityatlas.locationservice.web;

import com.accountabilityatlas.locationservice.service.LocationStatsService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/locations")
@RequiredArgsConstructor
public class InternalLocationController {

  private final LocationStatsService locationStatsService;

  public record LocationIdsRequest(List<UUID> locationIds) {}

  @PostMapping("/video-approved")
  public ResponseEntity<Void> videoApproved(@RequestBody LocationIdsRequest request) {
    locationStatsService.incrementVideoCount(request.locationIds());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/video-removed")
  public ResponseEntity<Void> videoRemoved(@RequestBody LocationIdsRequest request) {
    locationStatsService.decrementVideoCount(request.locationIds());
    return ResponseEntity.noContent().build();
  }
}
