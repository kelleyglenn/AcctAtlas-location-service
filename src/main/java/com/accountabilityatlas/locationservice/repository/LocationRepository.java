package com.accountabilityatlas.locationservice.repository;

import com.accountabilityatlas.locationservice.domain.Location;
import java.util.List;
import java.util.UUID;
import org.locationtech.jts.geom.Polygon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {

  @Query(
      """
      SELECT l FROM Location l
      LEFT JOIN FETCH l.stats
      WHERE ST_Within(l.coordinates, :bbox) = true
      """)
  List<Location> findWithinBoundingBox(@Param("bbox") Polygon bbox);
}
