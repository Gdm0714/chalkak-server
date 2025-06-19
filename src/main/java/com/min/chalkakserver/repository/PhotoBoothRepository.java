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
    
    // 이름으로 검색 (부분 일치, 대소문자 구분 안함)
    List<PhotoBooth> findByNameContainingIgnoreCase(String name);
    
    // 주소로 검색 (부분 일치, 대소문자 구분 안함)
    List<PhotoBooth> findByAddressContainingIgnoreCase(String address);
    
    // 이름 또는 주소로 검색
    List<PhotoBooth> findByNameContainingIgnoreCaseOrAddressContainingIgnoreCase(String name, String address);
    
    // 특정 좌표 근처의 네컷사진관 찾기 (반경 내 검색, 단위: km)
    @Query("SELECT p FROM PhotoBooth p WHERE " +
           "(6371 * acos(cos(radians(:latitude)) * cos(radians(p.latitude)) * " +
           "cos(radians(p.longitude) - radians(:longitude)) + " +
           "sin(radians(:latitude)) * sin(radians(p.latitude)))) <= :radius " +
           "ORDER BY (6371 * acos(cos(radians(:latitude)) * cos(radians(p.latitude)) * " +
           "cos(radians(p.longitude) - radians(:longitude)) + " +
           "sin(radians(:latitude)) * sin(radians(p.latitude))))")
    List<PhotoBooth> findByLocationNear(@Param("latitude") Double latitude, 
                                       @Param("longitude") Double longitude, 
                                       @Param("radius") Double radius);
    
    // 운영 시간 정보가 있는 네컷사진관 조회
    List<PhotoBooth> findByOperatingHoursIsNotNull();
    
    // 전화번호가 있는 네컷사진관 조회
    List<PhotoBooth> findByPhoneNumberIsNotNull();
}
