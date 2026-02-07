package com.accountabilityatlas.locationservice.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "locations", schema = "locations")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Location {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
  @NonNull
  private Point coordinates;

  @Column(name = "display_name", nullable = false, length = 255)
  @NonNull
  private String displayName;

  @Column(length = 500)
  private String address;

  @Column(length = 100)
  private String city;

  @Column(length = 100)
  private String state;

  @Column(length = 100)
  private String country;

  @Column(name = "sys_period", insertable = false, updatable = false)
  private Instant createdAt;

  @OneToOne(mappedBy = "location", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private LocationStats stats;

  public double getLatitude() {
    return coordinates.getY();
  }

  public double getLongitude() {
    return coordinates.getX();
  }
}
