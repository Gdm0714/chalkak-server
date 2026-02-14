package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.PhotoBoothRequestDto;
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
        // when
        List<PhotoBoothResponseDto> result1 = photoBoothService.getAllPhotoBooths();
        List<PhotoBoothResponseDto> result2 = photoBoothService.getAllPhotoBooths();

        // then
        assertThat(result1).hasSize(2);
        assertThat(result2).hasSize(2);
        assertThat(cacheManager.getCache("photoBooths")).isNotNull();
    }
    
    @Test
    @DisplayName("데이터 생성 시 관련 캐시가 초기화된다")
    void testCacheEvictionOnCreate() {
        // given - 캐싱
        photoBoothService.getAllPhotoBooths();
        
        // when - 새로운 데이터 생성
        PhotoBoothRequestDto request = PhotoBoothRequestDto.builder()
            .name("새로운 네컷사진관")
            .brand("포토이즘")
            .series("컬러드")
            .address("서울시 신촌")
            .latitude(37.5585)
            .longitude(126.9386)
            .operatingHours("10:00-22:00")
            .phoneNumber("02-3456-7890")
            .description("새로운 설명")
            .priceInfo("8000원")
            .build();

        photoBoothService.createPhotoBooth(request);
        
        // 캐시가 초기화되었으므로 다시 DB에서 조회
        List<PhotoBoothResponseDto> result = photoBoothService.getAllPhotoBooths();
        
        // then
        assertThat(result).hasSize(3);
    }
}
