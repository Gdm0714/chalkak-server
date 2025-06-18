package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.PhotoBoothRequestDto;
import com.min.chalkakserver.dto.PhotoBoothResponseDto;
import com.min.chalkakserver.entity.PhotoBooth;
import com.min.chalkakserver.repository.PhotoBoothRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PhotoBoothService {
    
    @Autowired
    private PhotoBoothRepository photoBoothRepository;
    
    // 모든 네컷사진관 조회
    @Transactional(readOnly = true)
    public List<PhotoBoothResponseDto> getAllPhotoBooths() {
        return photoBoothRepository.findAll()
                .stream()
                .map(PhotoBoothResponseDto::new)
                .collect(Collectors.toList());
    }
    
    // ID로 네컷사진관 조회
    @Transactional(readOnly = true)
    public PhotoBoothResponseDto getPhotoBoothById(Long id) {
        PhotoBooth photoBooth = photoBoothRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("네컷사진관을 찾을 수 없습니다. ID: " + id));
        return new PhotoBoothResponseDto(photoBooth);
    }
    
    // 네컷사진관 생성
    public PhotoBoothResponseDto createPhotoBooth(PhotoBoothRequestDto requestDto) {
        PhotoBooth photoBooth = new PhotoBooth(
                requestDto.getName(),
                requestDto.getBrand(),
                requestDto.getAddress(),
                requestDto.getRoadAddress(),
                requestDto.getLatitude(),
                requestDto.getLongitude(),
                requestDto.getOperatingHours(),
                requestDto.getPhoneNumber(),
                requestDto.getDescription(),
                requestDto.getPriceInfo()
        );
        
        PhotoBooth savedPhotoBooth = photoBoothRepository.save(photoBooth);
        return new PhotoBoothResponseDto(savedPhotoBooth);
    }
    
    // 네컷사진관 수정
    public PhotoBoothResponseDto updatePhotoBooth(Long id, PhotoBoothRequestDto requestDto) {
        PhotoBooth photoBooth = photoBoothRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("네컷사진관을 찾을 수 없습니다. ID: " + id));
        
        // 업데이트
        photoBooth.setName(requestDto.getName());
        photoBooth.setBrand(requestDto.getBrand());
        photoBooth.setAddress(requestDto.getAddress());
        photoBooth.setRoadAddress(requestDto.getRoadAddress());
        photoBooth.setLatitude(requestDto.getLatitude());
        photoBooth.setLongitude(requestDto.getLongitude());
        photoBooth.setOperatingHours(requestDto.getOperatingHours());
        photoBooth.setPhoneNumber(requestDto.getPhoneNumber());
        photoBooth.setDescription(requestDto.getDescription());
        photoBooth.setPriceInfo(requestDto.getPriceInfo());
        
        PhotoBooth updatedPhotoBooth = photoBoothRepository.save(photoBooth);
        return new PhotoBoothResponseDto(updatedPhotoBooth);
    }
    
    // 네컷사진관 삭제
    public void deletePhotoBooth(Long id) {
        if (!photoBoothRepository.existsById(id)) {
            throw new RuntimeException("네컷사진관을 찾을 수 없습니다. ID: " + id);
        }
        photoBoothRepository.deleteById(id);
    }
    
    // 키워드로 검색
    @Transactional(readOnly = true)
    public List<PhotoBoothResponseDto> searchPhotoBooths(String keyword) {
        return photoBoothRepository.findByKeyword(keyword)
                .stream()
                .map(PhotoBoothResponseDto::new)
                .collect(Collectors.toList());
    }
    
    // 브랜드로 검색
    @Transactional(readOnly = true)
    public List<PhotoBoothResponseDto> getPhotoBoothsByBrand(String brand) {
        return photoBoothRepository.findByBrandContainingIgnoreCase(brand)
                .stream()
                .map(PhotoBoothResponseDto::new)
                .collect(Collectors.toList());
    }
    
    // 근처 네컷사진관 검색 (기본 반경 5km)
    @Transactional(readOnly = true)
    public List<PhotoBoothResponseDto> getNearbyPhotoBooths(Double latitude, Double longitude, Double radius) {
        if (radius == null) {
            radius = 5.0; // 기본 반경 5km
        }
        return photoBoothRepository.findByLocationNear(latitude, longitude, radius)
                .stream()
                .map(PhotoBoothResponseDto::new)
                .collect(Collectors.toList());
    }
}
