package com.accountabilityatlas.locationservice.repository;

import com.accountabilityatlas.locationservice.domain.LocationStats;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationStatsRepository extends JpaRepository<LocationStats, UUID> {}
