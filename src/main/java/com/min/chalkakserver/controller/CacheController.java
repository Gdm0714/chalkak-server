package com.min.chalkakserver.controller;

import com.min.chalkakserver.service.CacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 캐시 관리 컨트롤러
 * 보안: API 키 인증 필요 (관리자 전용)
 */
@RestController
@RequestMapping("/api/cache")
public class CacheController {
    
    @Autowired
    private CacheService cacheService;
    
    @Value("${app.admin-api-key:}")
    private String adminApiKey;
    
    /**
     * API 키 검증
     * @param apiKey 요청에서 전달된 API 키
     * @return 유효한 API 키인지 여부
     */
    private boolean isValidApiKey(String apiKey) {
        // API 키가 설정되지 않았거나 비어있으면 모든 접근 차단
        if (adminApiKey == null || adminApiKey.isEmpty()) {
            return false;
        }
        return adminApiKey.equals(apiKey);
    }
    
    /**
     * 캐시 통계 조회 (관리자 API 키 필요)
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getCacheStatistics(
            @RequestHeader(value = "X-Admin-Api-Key", required = false) String apiKey) {
        if (!isValidApiKey(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "유효하지 않은 API 키입니다."));
        }
        Map<String, Object> statistics = cacheService.getCacheStatistics();
        return ResponseEntity.ok(statistics);
    }
    
    /**
     * 특정 캐시 초기화 (관리자 API 키 필요)
     */
    @DeleteMapping("/{cacheName}")
    public ResponseEntity<?> evictCache(
            @PathVariable String cacheName,
            @RequestHeader(value = "X-Admin-Api-Key", required = false) String apiKey) {
        if (!isValidApiKey(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "유효하지 않은 API 키입니다."));
        }
        cacheService.evictCache(cacheName);
        return ResponseEntity.ok(Map.of("message", cacheName + " 캐시가 초기화되었습니다."));
    }
    
    /**
     * 모든 캐시 초기화 (관리자 API 키 필요)
     */
    @DeleteMapping
    public ResponseEntity<?> evictAllCaches(
            @RequestHeader(value = "X-Admin-Api-Key", required = false) String apiKey) {
        if (!isValidApiKey(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "유효하지 않은 API 키입니다."));
        }
        cacheService.evictAllCaches();
        return ResponseEntity.ok(Map.of("message", "모든 캐시가 초기화되었습니다."));
    }
}
