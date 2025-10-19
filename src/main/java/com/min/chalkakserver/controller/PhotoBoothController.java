package com.min.chalkakserver.controller;

import com.min.chalkakserver.dto.PhotoBoothReportDto;
import com.min.chalkakserver.dto.PhotoBoothRequestDto;
import com.min.chalkakserver.dto.PhotoBoothResponseDto;
import com.min.chalkakserver.service.EmailService;
import com.min.chalkakserver.service.PhotoBoothService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/photo-booths")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Validated
public class PhotoBoothController {
    
    private final PhotoBoothService photoBoothService;

    private final EmailService emailService;
    
    @GetMapping
    public ResponseEntity<List<PhotoBoothResponseDto>> getAllPhotoBooths() {
        List<PhotoBoothResponseDto> photoBooths = photoBoothService.getAllPhotoBooths();
        return ResponseEntity.ok(photoBooths);
    }
    
    @GetMapping("/nearby")
    public ResponseEntity<List<PhotoBoothResponseDto>> getNearbyPhotoBooths(
            @RequestParam @Min(value = -90, message = "위도는 -90도 이상이어야 합니다") 
            @Max(value = 90, message = "위도는 90도 이하여야 합니다") double latitude,
            @RequestParam @Min(value = -180, message = "경도는 -180도 이상이어야 합니다") 
            @Max(value = 180, message = "경도는 180도 이하여야 합니다") double longitude,
            @RequestParam(defaultValue = "3.0") @Min(value = 0, message = "반경은 0km 이상이어야 합니다")
            @Max(value = 50, message = "반경은 50km 이하여야 합니다") double radius) {
        List<PhotoBoothResponseDto> nearbyBooths = photoBoothService.getNearbyPhotoBooths(latitude, longitude, radius);
        return ResponseEntity.ok(nearbyBooths);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PhotoBoothResponseDto> getPhotoBoothById(@PathVariable Long id) {
        PhotoBoothResponseDto photoBooth = photoBoothService.getPhotoBoothById(id);
        return ResponseEntity.ok(photoBooth);
    }

    @GetMapping("/search")
    public ResponseEntity<List<PhotoBoothResponseDto>> searchPhotoBooths(
            @RequestParam String keyword) {
        List<PhotoBoothResponseDto> searchResults = photoBoothService.searchPhotoBooths(keyword);
        return ResponseEntity.ok(searchResults);
    }
    
    @GetMapping("/brand/{brand}")
    public ResponseEntity<List<PhotoBoothResponseDto>> getPhotoBoothsByBrand(
            @PathVariable String brand) {
        List<PhotoBoothResponseDto> brandResults = photoBoothService.getPhotoBoothsByBrand(brand);
        return ResponseEntity.ok(brandResults);
    }

    @GetMapping("/brand/{brand}/series/{series}")
    public ResponseEntity<List<PhotoBoothResponseDto>> getPhotoBoothsByBrandAndSeries(
            @PathVariable String brand,
            @PathVariable String series) {
        List<PhotoBoothResponseDto> results = photoBoothService.getPhotoBoothsByBrandAndSeries(brand, series);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/series/{series}")
    public ResponseEntity<List<PhotoBoothResponseDto>> getPhotoBoothsBySeries(
            @PathVariable String series) {
        List<PhotoBoothResponseDto> seriesResults = photoBoothService.getPhotoBoothsBySeries(series);
        return ResponseEntity.ok(seriesResults);
    }

    @PostMapping("/report")
    @Operation(summary = "네컷사진관 제보", description = "사용자가 새로운 네컷사진관 정보를 관리자에게 제보합니다")
    public ResponseEntity<Map<String, String>> reportPhotoBooth(
        @Valid @RequestBody PhotoBoothReportDto reportDto) {
        emailService.sendPhotoBoothReport(reportDto);
        return ResponseEntity.ok(Map.of("message", "제보가 접수되었습니다. 감사합니다!"));
    }
}
