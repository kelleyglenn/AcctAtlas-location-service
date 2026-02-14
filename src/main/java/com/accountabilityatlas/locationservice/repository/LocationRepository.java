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

  @Query(
      value =
          """
          WITH clustered AS (
            SELECT
              id,
              coordinates,
              display_name,
              ST_ClusterDBSCAN(coordinates, eps := :eps, minpoints := 2) OVER () AS cluster_id
            FROM locations.locations
            WHERE ST_Within(coordinates, ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326))
          )
          SELECT
            ST_Y(ST_Centroid(ST_Collect(coordinates))) AS lat,
            ST_X(ST_Centroid(ST_Collect(coordinates))) AS lng,
            COUNT(*) AS count,
            cluster_id,
            MIN(ST_Y(coordinates)) AS min_lat,
            MAX(ST_Y(coordinates)) AS max_lat,
            MIN(ST_X(coordinates)) AS min_lng,
            MAX(ST_X(coordinates)) AS max_lng
          FROM clustered
          WHERE cluster_id IS NOT NULL
          GROUP BY cluster_id
          UNION ALL
          SELECT
            ST_Y(coordinates) AS lat,
            ST_X(coordinates) AS lng,
            1 AS count,
            NULL AS cluster_id,
            ST_Y(coordinates) AS min_lat,
            ST_Y(coordinates) AS max_lat,
            ST_X(coordinates) AS min_lng,
            ST_X(coordinates) AS max_lng
          FROM clustered
          WHERE cluster_id IS NULL
          """,
      nativeQuery = true)
  List<Object[]> findClustersInBoundingBox(
      @Param("minLng") double minLng,
      @Param("minLat") double minLat,
      @Param("maxLng") double maxLng,
      @Param("maxLat") double maxLat,
      @Param("eps") double eps);
}
