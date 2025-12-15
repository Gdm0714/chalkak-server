package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.PhotoBoothResponseDto;
import com.min.chalkakserver.entity.PhotoBooth;
import com.min.chalkakserver.repository.PhotoBoothRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PhotoBoothServiceCacheTest {
    
    @Autowired
    private PhotoBoothService photoBoothService;
    
    @Autowired
    private PhotoBoothRepository photoBoothRepository;
    
    @Autowired
    private CacheManager cacheManager;
    
    @BeforeEach
    void setUp() {
        // 캐시 초기화
        cacheManager.getCacheNames().forEach(cacheName -> 
            cacheManager.getCache(cacheName).clear()
        );
        
        // 테스트 데이터 생성
        PhotoBooth photoBooth1 = new PhotoBooth(
            "테스트 네컷사진관 1", "인생네컷", null, "서울시 강남구", null,
            37.5665, 126.9780, "10:00-22:00", "02-1234-5678",
            "테스트 설명", "6000원"
        );
        PhotoBooth photoBooth2 = new PhotoBooth(
            "테스트 네컷사진관 2", "포토이즘", "박스", "서울시 홍대", null,
            37.5565, 126.9239, "10:00-22:00", "02-2345-6789",
            "테스트 설명", "7000원"
        );
        
        photoBoothRepository.save(photoBooth1);
        photoBoothRepository.save(photoBooth2);
    }
    
    @Test
    @DisplayName("전체 네컷사진관 조회 시 캐싱이 적용된다")
    void testGetAllPhotoBoothsCaching() {
        // given
        long startTime1 = System.currentTimeMillis();
        
        // when - 첫 번째 호출 (캐시 미스)
        List<PhotoBoothResponseDto> result1 = photoBoothService.getAllPhotoBooths();
        long executionTime1 = System.currentTimeMillis() - startTime1;
        
        // then
        assertThat(result1).hasSize(2);
        
        // when - 두 번째 호출 (캐시 히트)
        long startTime2 = System.currentTimeMillis();
        List<PhotoBoothResponseDto> result2 = photoBoothService.getAllPhotoBooths();
        long executionTime2 = System.currentTimeMillis() - startTime2;
        
        // then - 캐시 히트로 인해 두 번째 호출이 더 빠름
        assertThat(result2).hasSize(2);
        assertThat(executionTime2).isLessThan(executionTime1);
        System.out.println("첫 번째 호출 시간: " + executionTime1 + "ms");
        System.out.println("두 번째 호출 시간: " + executionTime2 + "ms");
    }
    
    @Test
    @DisplayName("위치 기반 검색 시 캐싱이 적용된다")
    void testGetNearbyPhotoBoothsCaching() {
        // given
        double latitude = 37.5665;
        double longitude = 126.9780;
        double radius = 5.0;
        
        // when - 첫 번째 호출
        long startTime1 = System.currentTimeMillis();
        List<PhotoBoothResponseDto> result1 = photoBoothService.getNearbyPhotoBooths(latitude, longitude, radius);
        long executionTime1 = System.currentTimeMillis() - startTime1;
        
        // when - 두 번째 호출 (동일한 파라미터)
        long startTime2 = System.currentTimeMillis();
        List<PhotoBoothResponseDto> result2 = photoBoothService.getNearbyPhotoBooths(latitude, longitude, radius);
        long executionTime2 = System.currentTimeMillis() - startTime2;
        
        // then
        assertThat(result1).isNotEmpty();
        assertThat(result2).isEqualTo(result1);
        assertThat(executionTime2).isLessThan(executionTime1);
    }
    
    @Test
    @DisplayName("데이터 생성 시 관련 캐시가 초기화된다")
    void testCacheEvictionOnCreate() {
        // given - 캐싱
        photoBoothService.getAllPhotoBooths();
        
        // when - 새로운 데이터 생성
        PhotoBooth newPhotoBooth = new PhotoBooth(
            "새로운 네컷사진관", "포토이즘", "컬러드", "서울시 신촌", null,
            37.5585, 126.9386, "10:00-22:00", "02-3456-7890",
            "새로운 설명", "8000원"
        );
        photoBoothRepository.save(newPhotoBooth);
        
        // 캐시가 초기화되었으므로 다시 DB에서 조회
        List<PhotoBoothResponseDto> result = photoBoothService.getAllPhotoBooths();
        
        // then
        assertThat(result).hasSize(3);
    }
}
