package com.min.chalkakserver.controller;

import com.min.chalkakserver.service.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CacheController 테스트")
class CacheControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private CacheController cacheController;

    @BeforeEach
    void setUp() throws Exception {
        // adminApiKey 설정
        Field field = CacheController.class.getDeclaredField("adminApiKey");
        field.setAccessible(true);
        field.set(cacheController, "test-admin-key");

        mockMvc = MockMvcBuilders.standaloneSetup(cacheController).build();
    }

    @Nested
    @DisplayName("캐시 통계 조회")
    class GetCacheStatisticsTest {

        @Test
        @DisplayName("유효한 API 키로 캐시 통계를 조회한다")
        void getCacheStatistics_ValidKey_Success() throws Exception {
            Map<String, Object> stats = new HashMap<>();
            stats.put("photoBooths", Map.of("size", 10));
            given(cacheService.getCacheStatistics()).willReturn(stats);

            mockMvc.perform(get("/api/cache/statistics")
                            .header("X-Admin-Api-Key", "test-admin-key"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.photoBooths.size").value(10));
        }

        @Test
        @DisplayName("유효하지 않은 API 키로 조회 시 401을 반환한다")
        void getCacheStatistics_InvalidKey_Unauthorized() throws Exception {
            mockMvc.perform(get("/api/cache/statistics")
                            .header("X-Admin-Api-Key", "wrong-key"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("유효하지 않은 API 키입니다."));
        }

        @Test
        @DisplayName("API 키 없이 조회 시 401을 반환한다")
        void getCacheStatistics_NoKey_Unauthorized() throws Exception {
            mockMvc.perform(get("/api/cache/statistics"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("유효하지 않은 API 키입니다."));
        }
    }

    @Nested
    @DisplayName("특정 캐시 초기화")
    class EvictCacheTest {

        @Test
        @DisplayName("유효한 API 키로 특정 캐시를 초기화한다")
        void evictCache_ValidKey_Success() throws Exception {
            mockMvc.perform(delete("/api/cache/{cacheName}", "photoBooths")
                            .header("X-Admin-Api-Key", "test-admin-key"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("photoBooths 캐시가 초기화되었습니다."));

            verify(cacheService).evictCache("photoBooths");
        }

        @Test
        @DisplayName("유효하지 않은 API 키로 초기화 시 401을 반환한다")
        void evictCache_InvalidKey_Unauthorized() throws Exception {
            mockMvc.perform(delete("/api/cache/{cacheName}", "photoBooths")
                            .header("X-Admin-Api-Key", "wrong-key"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("모든 캐시 초기화")
    class EvictAllCachesTest {

        @Test
        @DisplayName("유효한 API 키로 모든 캐시를 초기화한다")
        void evictAllCaches_ValidKey_Success() throws Exception {
            mockMvc.perform(delete("/api/cache")
                            .header("X-Admin-Api-Key", "test-admin-key"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("모든 캐시가 초기화되었습니다."));

            verify(cacheService).evictAllCaches();
        }

        @Test
        @DisplayName("유효하지 않은 API 키로 초기화 시 401을 반환한다")
        void evictAllCaches_InvalidKey_Unauthorized() throws Exception {
            mockMvc.perform(delete("/api/cache")
                            .header("X-Admin-Api-Key", "wrong-key"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("API 키 검증")
    class ApiKeyValidationTest {

        @Test
        @DisplayName("adminApiKey가 비어있으면 모든 접근을 차단한다")
        void emptyAdminApiKey_BlocksAll() throws Exception {
            // given - adminApiKey를 빈 문자열로 설정
            Field field = CacheController.class.getDeclaredField("adminApiKey");
            field.setAccessible(true);
            field.set(cacheController, "");

            mockMvc.perform(get("/api/cache/statistics")
                            .header("X-Admin-Api-Key", ""))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("adminApiKey가 null이면 모든 접근을 차단한다")
        void nullAdminApiKey_BlocksAll() throws Exception {
            // given
            Field field = CacheController.class.getDeclaredField("adminApiKey");
            field.setAccessible(true);
            field.set(cacheController, null);

            mockMvc.perform(get("/api/cache/statistics"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
