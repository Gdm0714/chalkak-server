package com.min.chalkakserver.controller;

import com.min.chalkakserver.dto.PagedResponseDto;
import com.min.chalkakserver.dto.PhotoBoothReportDto;
import com.min.chalkakserver.dto.PhotoBoothRequestDto;
import com.min.chalkakserver.dto.PhotoBoothResponseDto;
import com.min.chalkakserver.service.EmailService;
import com.min.chalkakserver.service.PhotoBoothService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Map;
import lombok.RequiredArgsConstructor;
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
    @Operation(summary = "모든 네컷사진관 조회", description = "전체 네컷사진관 목록을 조회합니다")
    public ResponseEntity<List<PhotoBoothResponseDto>> getAllPhotoBooths() {
        List<PhotoBoothResponseDto> photoBooths = photoBoothService.getAllPhotoBooths();
        return ResponseEntity.ok(photoBooths);
    }
    
    @GetMapping("/paged")
    @Operation(summary = "모든 네컷사진관 조회 (페이지네이션)", description = "페이지네이션을 적용하여 네컷사진관 목록을 조회합니다")
    public ResponseEntity<PagedResponseDto<PhotoBoothResponseDto>> getAllPhotoBoothsPaged(
            @Parameter(description = "페이지 번호 (0부터 시작)") 
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기 (최대 100)") 
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        PagedResponseDto<PhotoBoothResponseDto> photoBooths = photoBoothService.getAllPhotoBoothsPaged(page, size);
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
    @Operation(summary = "키워드로 검색", description = "이름 또는 주소에 키워드가 포함된 네컷사진관을 검색합니다")
    public ResponseEntity<List<PhotoBoothResponseDto>> searchPhotoBooths(
            @RequestParam String keyword) {
        List<PhotoBoothResponseDto> searchResults = photoBoothService.searchPhotoBooths(keyword);
        return ResponseEntity.ok(searchResults);
    }
    
    @GetMapping("/search/paged")
    @Operation(summary = "키워드로 검색 (페이지네이션)", description = "페이지네이션을 적용하여 키워드 검색 결과를 조회합니다")
    public ResponseEntity<PagedResponseDto<PhotoBoothResponseDto>> searchPhotoBoothsPaged(
            @Parameter(description = "검색 키워드") @RequestParam String keyword,
            @Parameter(description = "페이지 번호 (0부터 시작)") 
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기 (최대 100)") 
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        PagedResponseDto<PhotoBoothResponseDto> searchResults = photoBoothService.searchPhotoBoothsPaged(keyword, page, size);
        return ResponseEntity.ok(searchResults);
    }
    
    @GetMapping("/brand/{brand}")
    @Operation(summary = "브랜드별 조회", description = "특정 브랜드의 네컷사진관을 조회합니다")
    public ResponseEntity<List<PhotoBoothResponseDto>> getPhotoBoothsByBrand(
            @PathVariable String brand) {
        List<PhotoBoothResponseDto> brandResults = photoBoothService.getPhotoBoothsByBrand(brand);
        return ResponseEntity.ok(brandResults);
    }
    
    @GetMapping("/brand/{brand}/paged")
    @Operation(summary = "브랜드별 조회 (페이지네이션)", description = "페이지네이션을 적용하여 특정 브랜드의 네컷사진관을 조회합니다")
    public ResponseEntity<PagedResponseDto<PhotoBoothResponseDto>> getPhotoBoothsByBrandPaged(
            @PathVariable String brand,
            @Parameter(description = "페이지 번호 (0부터 시작)") 
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기 (최대 100)") 
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        PagedResponseDto<PhotoBoothResponseDto> brandResults = photoBoothService.getPhotoBoothsByBrandPaged(brand, page, size);
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
