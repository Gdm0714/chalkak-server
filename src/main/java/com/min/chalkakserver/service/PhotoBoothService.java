package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.PhotoBoothRequestDto;
import com.min.chalkakserver.dto.PhotoBoothResponseDto;
import com.min.chalkakserver.entity.PhotoBooth;
import com.min.chalkakserver.exception.InvalidLocationException;
import com.min.chalkakserver.exception.PhotoBoothNotFoundException;
import com.min.chalkakserver.repository.PhotoBoothRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PhotoBoothService {
    
    private final PhotoBoothRepository photoBoothRepository;
    
    // 모든 네컷사진관 조회
    @Transactional(readOnly = true)
    @Cacheable(value = "photoBooths", key = "#root.target + #root.methodName", unless = "#result == null || #result.isEmpty()")
    public List<PhotoBoothResponseDto> getAllPhotoBooths() {
        log.info("모든 네컷사진관 조회 - DB에서 데이터 조회");
        return photoBoothRepository.findAll()
                .stream()
                .map(PhotoBoothResponseDto::from)
                .collect(Collectors.toList());
    }
    
    // ID로 네컷사진관 조회
    @Transactional(readOnly = true)
    @Cacheable(value = "photoBooth", key = "#id", unless = "#result == null")
    public PhotoBoothResponseDto getPhotoBoothById(Long id) {
        log.info("ID {} 로 네컷사진관 조회 - DB에서 데이터 조회", id);
        PhotoBooth photoBooth = photoBoothRepository.findById(id)
                .orElseThrow(() -> new PhotoBoothNotFoundException(id));
        return PhotoBoothResponseDto.from(photoBooth);
    }
    
    // 근처 네컷사진관 검색
    @Transactional(readOnly = true)
    @Cacheable(value = "nearbyPhotoBooths", keyGenerator = "locationKeyGenerator", unless = "#result == null || #result.isEmpty()")
    public List<PhotoBoothResponseDto> getNearbyPhotoBooths(double latitude, double longitude, double radius) {
        log.info("근처 네컷사진관 검색 - 위도: {}, 경도: {}, 반경: {}km - DB에서 데이터 조회", latitude, longitude, radius);
        
        // 위치 유효성 검증
        validateLocation(latitude, longitude, radius);
        
        // Bounding Box 계산 (대략적인 범위 필터링)
        double latRange = radius / 111.0; // 1도 = 약 111km
        double lonRange = radius / (111.0 * Math.cos(Math.toRadians(latitude)));
        
        double minLat = latitude - latRange;
        double maxLat = latitude + latRange;
        double minLon = longitude - lonRange;
        double maxLon = longitude + lonRange;
        
        // Spatial Index를 활용한 쿼리 실행
        List<PhotoBooth> nearbyBooths = photoBoothRepository.findNearbyPhotoBooths(
            latitude, longitude, radius, minLat, maxLat, minLon, maxLon
        );
        
        return nearbyBooths.stream()
                .map(PhotoBoothResponseDto::from)
                .collect(Collectors.toList());
    }
    
    // 네컷사진관 생성
    @Caching(evict = {
        @CacheEvict(value = "photoBooths", allEntries = true),
        @CacheEvict(value = "nearbyPhotoBooths", allEntries = true),
        @CacheEvict(value = "searchResults", allEntries = true),
        @CacheEvict(value = "brandPhotoBooths", allEntries = true)
    })
    public PhotoBoothResponseDto createPhotoBooth(PhotoBoothRequestDto requestDto) {
        log.info("네컷사진관 생성 - 이름: {}", requestDto.getName());
        
        // DTO를 Entity로 변환
        PhotoBooth photoBooth = requestDto.toEntity();
        
        PhotoBooth savedPhotoBooth = photoBoothRepository.save(photoBooth);
        
        // Entity를 DTO로 변환하여 반환
        return PhotoBoothResponseDto.from(savedPhotoBooth);
    }
    
    // 네컷사진관 수정
    @Caching(evict = {
        @CacheEvict(value = "photoBooths", allEntries = true),
        @CacheEvict(value = "photoBooth", key = "#id"),
        @CacheEvict(value = "nearbyPhotoBooths", allEntries = true),
        @CacheEvict(value = "searchResults", allEntries = true),
        @CacheEvict(value = "brandPhotoBooths", allEntries = true)
    })
    public PhotoBoothResponseDto updatePhotoBooth(Long id, PhotoBoothRequestDto requestDto) {
        log.info("네컷사진관 수정 - ID: {}", id);
        
        PhotoBooth photoBooth = photoBoothRepository.findById(id)
                .orElseThrow(() -> new PhotoBoothNotFoundException(id));
        
        // update 메서드를 사용하여 엔티티 업데이트
        photoBooth.update(
            requestDto.getName(),
            requestDto.getBrand(),
            requestDto.getSeries(),
            requestDto.getAddress(),
            requestDto.getRoadAddress(),
            requestDto.getLatitude(),
            requestDto.getLongitude(),
            requestDto.getOperatingHours(),
            requestDto.getPhoneNumber(),
            requestDto.getDescription(),
            requestDto.getPriceInfo()
        );
        
        PhotoBooth updatedPhotoBooth = photoBoothRepository.save(photoBooth);
        return PhotoBoothResponseDto.from(updatedPhotoBooth);
    }
    
    // 네컷사진관 삭제
    @Caching(evict = {
        @CacheEvict(value = "photoBooths", allEntries = true),
        @CacheEvict(value = "photoBooth", key = "#id"),
        @CacheEvict(value = "nearbyPhotoBooths", allEntries = true),
        @CacheEvict(value = "searchResults", allEntries = true),
        @CacheEvict(value = "brandPhotoBooths", allEntries = true)
    })
    public void deletePhotoBooth(Long id) {
        log.info("네컷사진관 삭제 - ID: {}", id);
        if (!photoBoothRepository.existsById(id)) {
            throw new PhotoBoothNotFoundException(id);
        }
        photoBoothRepository.deleteById(id);
    }
    
    // 키워드로 검색
    @Transactional(readOnly = true)
    @Cacheable(value = "searchResults", key = "#keyword", unless = "#result == null || #result.isEmpty()")
    public List<PhotoBoothResponseDto> searchPhotoBooths(String keyword) {
        log.info("키워드로 검색 - 키워드: {} - DB에서 데이터 조회", keyword);
        return photoBoothRepository.findByNameContainingIgnoreCaseOrAddressContainingIgnoreCase(keyword, keyword)
                .stream()
                .map(PhotoBoothResponseDto::from)
                .collect(Collectors.toList());
    }
    
    // 브랜드로 검색
    @Transactional(readOnly = true)
    @Cacheable(value = "brandPhotoBooths", key = "#brand", unless = "#result == null || #result.isEmpty()")
    public List<PhotoBoothResponseDto> getPhotoBoothsByBrand(String brand) {
        log.info("브랜드로 검색 - 브랜드: {} - DB에서 데이터 조회", brand);
        return photoBoothRepository.findByBrandContainingIgnoreCase(brand)
                .stream()
                .map(PhotoBoothResponseDto::from)
                .collect(Collectors.toList());
    }

    // 브랜드 + 시리즈로 검색
    @Transactional(readOnly = true)
    @Cacheable(value = "brandSeriesPhotoBooths", key = "#brand + '_' + #series", unless = "#result == null || #result.isEmpty()")
    public List<PhotoBoothResponseDto> getPhotoBoothsByBrandAndSeries(String brand, String series) {
        log.info("브랜드 + 시리즈로 검색 - 브랜드: {}, 시리즈: {} - DB에서 데이터 조회", brand, series);
        return photoBoothRepository.findByBrandContainingIgnoreCaseAndSeriesContainingIgnoreCase(brand, series)
                .stream()
                .map(PhotoBoothResponseDto::from)
                .collect(Collectors.toList());
    }
    
    // 시리즈로만 검색
    @Transactional(readOnly = true)
    @Cacheable(value = "seriesPhotoBooths", key = "#series", unless = "#result == null || #result.isEmpty()")
    public List<PhotoBoothResponseDto> getPhotoBoothsBySeries(String series) {
        log.info("시리즈로 검색 - 시리즈: {} - DB에서 데이터 조회", series);
        return photoBoothRepository.findBySeriesContainingIgnoreCase(series)
                .stream()
                .map(PhotoBoothResponseDto::from)
                .collect(Collectors.toList());
    }
    
    // 위치 유효성 검증 메서드
    private void validateLocation(double latitude, double longitude, double radius) {
        if (latitude < -90 || latitude > 90) {
            throw new InvalidLocationException("위도는 -90도에서 90도 사이여야 합니다. 입력값: " + latitude);
        }
        if (longitude < -180 || longitude > 180) {
            throw new InvalidLocationException("경도는 -180도에서 180도 사이여야 합니다. 입력값: " + longitude);
        }
        if (radius <= 0 || radius > 50) {
            throw new InvalidLocationException("검색 반경은 0km 초과 50km 이하여야 합니다. 입력값: " + radius);
        }
    }
}
