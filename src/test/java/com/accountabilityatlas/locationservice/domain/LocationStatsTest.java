package com.accountabilityatlas.locationservice.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LocationStatsTest {

  @Test
  void shouldCreateStatsWithZeroVideoCount() {
    LocationStats stats = new LocationStats();
    stats.setVideoCount(0);
    assertThat(stats.getVideoCount()).isZero();
  }

  @Test
  void shouldIncrementVideoCount() {
    LocationStats stats = new LocationStats();
    stats.setVideoCount(5);
    stats.incrementVideoCount();
    assertThat(stats.getVideoCount()).isEqualTo(6);
  }

  @Test
  void shouldDecrementVideoCount() {
    LocationStats stats = new LocationStats();
    stats.setVideoCount(5);
    stats.decrementVideoCount();
    assertThat(stats.getVideoCount()).isEqualTo(4);
  }

  @Test
  void shouldNotDecrementBelowZero() {
    LocationStats stats = new LocationStats();
    stats.setVideoCount(0);
    stats.decrementVideoCount();
    assertThat(stats.getVideoCount()).isZero();
  }
}
