package com.accountabilityatlas.locationservice.event;

import com.accountabilityatlas.locationservice.service.LocationStatsService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** SQS listener for video status change events from video-service. */
@Component
@RequiredArgsConstructor
@Slf4j
public class VideoStatusChangedHandler {

  private final LocationStatsService locationStatsService;

  @SqsListener("${app.sqs.video-status-events-queue:video-status-events}")
  public void handleVideoStatusChanged(VideoStatusChangedEvent event) {
    log.info(
        "Received VideoStatusChanged event for video {} ({} -> {})",
        event.videoId(),
        event.previousStatus(),
        event.newStatus());
    try {
      if ("APPROVED".equals(event.newStatus())) {
        locationStatsService.incrementVideoCount(event.locationIds());
      } else if ("APPROVED".equals(event.previousStatus())) {
        locationStatsService.decrementVideoCount(event.locationIds());
      }
    } catch (Exception e) {
      log.error(
          "Failed to handle VideoStatusChanged event for video {}: {}",
          event.videoId(),
          e.getMessage(),
          e);
      throw e;
    }
  }
}
