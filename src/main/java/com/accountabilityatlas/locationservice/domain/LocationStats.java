package com.accountabilityatlas.locationservice.domain;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "location_stats", schema = "locations")
@Getter
@Setter
@NoArgsConstructor
public class LocationStats {

  @Id
  @Column(name = "location_id")
  private UUID locationId;

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "location_id")
  private Location location;

  @Column(name = "video_count", nullable = false)
  private int videoCount;

  public void incrementVideoCount() {
    this.videoCount++;
  }

  public void decrementVideoCount() {
    if (this.videoCount > 0) {
      this.videoCount--;
    }
  }
}
