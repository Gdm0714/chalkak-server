package com.min.chalkakserver.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class CacheService {
    
    @Autowired
    private CacheManager cacheManager;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void evictCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
    
    public void evictAllCaches() {
        Collection<String> cacheNames = cacheManager.getCacheNames();
        cacheNames.forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }
    
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        cacheManager.getCacheNames().forEach(cacheName -> {
            Map<String, Object> cacheStats = new HashMap<>();
            Cache cache = cacheManager.getCache(cacheName);
            
            if (cache != null) {
                cacheStats.put("name", cacheName);
                cacheStats.put("type", cache.getClass().getSimpleName());
                
                // Redis 통계 정보
                Set<String> keys = redisTemplate.keys(cacheName + "::*");
                if (keys != null) {
                    cacheStats.put("size", keys.size());
                    cacheStats.put("keys", keys);
                    
                    // 각 키의 TTL 정보
                    Map<String, Long> ttlInfo = new HashMap<>();
                    keys.forEach(key -> {
                        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                        ttlInfo.put(key, ttl);
                    });
                    cacheStats.put("ttlInfo", ttlInfo);
                }
            }
            
            stats.put(cacheName, cacheStats);
        });
        
        return stats;
    }
    
    public void warmUpCache() {
    }
}
