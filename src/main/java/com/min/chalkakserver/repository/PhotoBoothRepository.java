package com.min.chalkakserver.repository;

import com.min.chalkakserver.entity.PhotoBooth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhotoBoothRepository extends JpaRepository<PhotoBooth, Long> {
    
    // 브랜드로 검색
    List<PhotoBooth> findByBrandContainingIgnoreCase(String brand);

    // 브랜드 + 시리즈로 검색
    List<PhotoBooth> findByBrandAndSeries(String brand, String series);

    // 브랜드 + 시리즈 (대소문자 무시)
    List<PhotoBooth> findByBrandContainingIgnoreCaseAndSeriesContainingIgnoreCase(String brand, String series);

    // 시리즈로만 검색
    List<PhotoBooth> findBySeriesContainingIgnoreCase(String series);

    // 이름 또는 주소로 검색
    List<PhotoBooth> findByNameContainingIgnoreCaseOrAddressContainingIgnoreCase(String name, String address);
    
    // Spatial Index를 활용한 근처 네컷사진관 검색
    // Bounding Box를 이용한 1차 필터링 후 정확한 거리 계산
    @Query(value = """
        SELECT pb.*, 
               (6371 * acos(cos(radians(:latitude)) * cos(radians(pb.latitude)) * 
                cos(radians(pb.longitude) - radians(:longitude)) + 
                sin(radians(:latitude)) * sin(radians(pb.latitude)))) AS distance 
        FROM photo_booths pb 
        WHERE pb.latitude BETWEEN :minLat AND :maxLat 
          AND pb.longitude BETWEEN :minLon AND :maxLon 
        HAVING distance <= :radius 
        ORDER BY distance
        """, nativeQuery = true)
    List<PhotoBooth> findNearbyPhotoBooths(
        @Param("latitude") double latitude,
        @Param("longitude") double longitude,
        @Param("radius") double radius,
        @Param("minLat") double minLat,
        @Param("maxLat") double maxLat,
        @Param("minLon") double minLon,
        @Param("maxLon") double maxLon
    );
    
    // 간단한 버전 - Point 타입 사용 시
    @Query(value = """
        SELECT *, ST_Distance_Sphere(
            POINT(longitude, latitude), 
            POINT(:longitude, :latitude)
        ) / 1000 AS distance 
        FROM photo_booths 
        WHERE ST_Distance_Sphere(
            POINT(longitude, latitude), 
            POINT(:longitude, :latitude)
        ) <= :radius * 1000 
        ORDER BY distance
        """, nativeQuery = true)
    List<PhotoBooth> findWithinRadius(
        @Param("latitude") double latitude,
        @Param("longitude") double longitude,
        @Param("radius") double radiusInKm
    );
}
