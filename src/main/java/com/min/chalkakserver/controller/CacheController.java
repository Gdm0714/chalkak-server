package com.min.chalkakserver.controller;

import com.min.chalkakserver.service.CacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cache")
public class CacheController {
    
    @Autowired
    private CacheService cacheService;
    
    /**
     * 캐시 통계 조회
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getCacheStatistics() {
        Map<String, Object> statistics = cacheService.getCacheStatistics();
        return ResponseEntity.ok(statistics);
    }
    
    /**
     * 특정 캐시 초기화
     */
    @DeleteMapping("/{cacheName}")
    public ResponseEntity<String> evictCache(@PathVariable String cacheName) {
        cacheService.evictCache(cacheName);
        return ResponseEntity.ok(cacheName + " 캐시가 초기화되었습니다.");
    }
    
    /**
     * 모든 캐시 초기화
     */
    @DeleteMapping
    public ResponseEntity<String> evictAllCaches() {
        cacheService.evictAllCaches();
        return ResponseEntity.ok("모든 캐시가 초기화되었습니다.");
    }
}
