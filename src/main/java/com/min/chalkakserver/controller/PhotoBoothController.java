package com.min.chalkakserver.controller;

import com.min.chalkakserver.dto.PhotoBoothRequestDto;
import com.min.chalkakserver.dto.PhotoBoothResponseDto;
import com.min.chalkakserver.service.PhotoBoothService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/photo-booths")
@CrossOrigin(origins = "*")
public class PhotoBoothController {
    
    @Autowired
    private PhotoBoothService photoBoothService;
    
    // 모든 네컷사진관 조회
    @GetMapping
    public ResponseEntity<List<PhotoBoothResponseDto>> getAllPhotoBooths() {
        List<PhotoBoothResponseDto> photoBooths = photoBoothService.getAllPhotoBooths();
        return ResponseEntity.ok(photoBooths);
    }
    
    // ID로 네컷사진관 조회
    @GetMapping("/{id}")
    public ResponseEntity<PhotoBoothResponseDto> getPhotoBoothById(@PathVariable Long id) {
        try {
            PhotoBoothResponseDto photoBooth = photoBoothService.getPhotoBoothById(id);
            return ResponseEntity.ok(photoBooth);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // 네컷사진관 생성
    @PostMapping
    public ResponseEntity<PhotoBoothResponseDto> createPhotoBooth(
            @Valid @RequestBody PhotoBoothRequestDto requestDto) {
        try {
            PhotoBoothResponseDto createdPhotoBooth = photoBoothService.createPhotoBooth(requestDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPhotoBooth);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 네컷사진관 수정
    @PutMapping("/{id}")
    public ResponseEntity<PhotoBoothResponseDto> updatePhotoBooth(
            @PathVariable Long id, 
            @Valid @RequestBody PhotoBoothRequestDto requestDto) {
        try {
            PhotoBoothResponseDto updatedPhotoBooth = photoBoothService.updatePhotoBooth(id, requestDto);
            return ResponseEntity.ok(updatedPhotoBooth);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 네컷사진관 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePhotoBooth(@PathVariable Long id) {
        try {
            photoBoothService.deletePhotoBooth(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // 키워드로 검색
    @GetMapping("/search")
    public ResponseEntity<List<PhotoBoothResponseDto>> searchPhotoBooths(
            @RequestParam String keyword) {
        List<PhotoBoothResponseDto> photoBooths = photoBoothService.searchPhotoBooths(keyword);
        return ResponseEntity.ok(photoBooths);
    }
    
    // 브랜드로 검색
    @GetMapping("/brand/{brand}")
    public ResponseEntity<List<PhotoBoothResponseDto>> getPhotoBoothsByBrand(
            @PathVariable String brand) {
        List<PhotoBoothResponseDto> photoBooths = photoBoothService.getPhotoBoothsByBrand(brand);
        return ResponseEntity.ok(photoBooths);
    }
    
    // 근처 네컷사진관 검색
    @GetMapping("/nearby")
    public ResponseEntity<List<PhotoBoothResponseDto>> getNearbyPhotoBooths(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false, defaultValue = "5.0") Double radius) {
        List<PhotoBoothResponseDto> photoBooths = photoBoothService.getNearbyPhotoBooths(latitude, longitude, radius);
        return ResponseEntity.ok(photoBooths);
    }
}
