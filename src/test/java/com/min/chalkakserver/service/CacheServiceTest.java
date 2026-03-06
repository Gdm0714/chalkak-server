package com.min.chalkakserver.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CacheService 테스트")
class CacheServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private CacheService cacheService;

    @Test
    @DisplayName("특정 캐시를 초기화한다")
    void evictCache_Success() {
        // given
        Cache cache = mock(Cache.class);
        given(cacheManager.getCache("photoBooths")).willReturn(cache);

        // when
        cacheService.evictCache("photoBooths");

        // then
        verify(cache).clear();
    }

    @Test
    @DisplayName("존재하지 않는 캐시 초기화 시 NPE 없이 무시한다")
    void evictCache_NotFound() {
        // given
        given(cacheManager.getCache("nonexistent")).willReturn(null);

        // when
        cacheService.evictCache("nonexistent");

        // then - no exception
    }

    @Test
    @DisplayName("모든 캐시를 초기화한다")
    void evictAllCaches_Success() {
        // given
        Cache cache1 = mock(Cache.class);
        Cache cache2 = mock(Cache.class);
        given(cacheManager.getCacheNames()).willReturn(Arrays.asList("photoBooths", "photoBooth"));
        given(cacheManager.getCache("photoBooths")).willReturn(cache1);
        given(cacheManager.getCache("photoBooth")).willReturn(cache2);

        // when
        cacheService.evictAllCaches();

        // then
        verify(cache1).clear();
        verify(cache2).clear();
    }

    @Test
    @DisplayName("캐시가 없을 때 모든 캐시 초기화 시 정상 처리된다")
    void evictAllCaches_NoCaches() {
        // given
        given(cacheManager.getCacheNames()).willReturn(Collections.emptyList());

        // when
        cacheService.evictAllCaches();

        // then - no exception
    }

    @Test
    @DisplayName("캐시 통계를 조회한다")
    void getCacheStatistics_Success() {
        // given
        Cache cache = mock(Cache.class);
        given(cacheManager.getCacheNames()).willReturn(Arrays.asList("photoBooths"));
        given(cacheManager.getCache("photoBooths")).willReturn(cache);

        Set<String> keys = Set.of("photoBooths::key1", "photoBooths::key2");
        given(redisTemplate.keys("photoBooths::*")).willReturn(keys);
        given(redisTemplate.getExpire("photoBooths::key1", TimeUnit.SECONDS)).willReturn(3600L);
        given(redisTemplate.getExpire("photoBooths::key2", TimeUnit.SECONDS)).willReturn(1800L);

        // when
        Map<String, Object> stats = cacheService.getCacheStatistics();

        // then
        assertThat(stats).containsKey("photoBooths");
        @SuppressWarnings("unchecked")
        Map<String, Object> cacheStats = (Map<String, Object>) stats.get("photoBooths");
        assertThat(cacheStats.get("name")).isEqualTo("photoBooths");
        assertThat(cacheStats.get("size")).isEqualTo(2);
    }

    @Test
    @DisplayName("캐시 통계 조회 시 키가 없으면 null keys를 처리한다")
    void getCacheStatistics_NullKeys() {
        // given
        Cache cache = mock(Cache.class);
        given(cacheManager.getCacheNames()).willReturn(Arrays.asList("photoBooths"));
        given(cacheManager.getCache("photoBooths")).willReturn(cache);
        given(redisTemplate.keys("photoBooths::*")).willReturn(null);

        // when
        Map<String, Object> stats = cacheService.getCacheStatistics();

        // then
        assertThat(stats).containsKey("photoBooths");
        @SuppressWarnings("unchecked")
        Map<String, Object> cacheStats = (Map<String, Object>) stats.get("photoBooths");
        assertThat(cacheStats).doesNotContainKey("size");
    }

    @Test
    @DisplayName("캐시 통계 조회 시 캐시가 null이면 빈 통계를 넣는다")
    void getCacheStatistics_NullCache() {
        // given
        given(cacheManager.getCacheNames()).willReturn(Arrays.asList("missingCache"));
        given(cacheManager.getCache("missingCache")).willReturn(null);

        // when
        Map<String, Object> stats = cacheService.getCacheStatistics();

        // then
        assertThat(stats).containsKey("missingCache");
        @SuppressWarnings("unchecked")
        Map<String, Object> cacheStats = (Map<String, Object>) stats.get("missingCache");
        assertThat(cacheStats).doesNotContainKey("name");
    }

    @Test
    @DisplayName("캐시 워밍업 메서드가 정상 호출된다")
    void warmUpCache_Success() {
        // when & then - 빈 메서드이므로 예외 없이 호출만 확인
        cacheService.warmUpCache();
    }
}
