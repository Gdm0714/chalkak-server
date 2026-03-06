package com.min.chalkakserver.config.cache;

import com.min.chalkakserver.service.PhotoBoothService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CacheWarmupRunner 테스트")
class CacheWarmupRunnerTest {

    @Mock
    private PhotoBoothService photoBoothService;

    @InjectMocks
    private CacheWarmupRunner cacheWarmupRunner;

    @Test
    @DisplayName("앱 시작 시 캐시 워밍업을 실행한다")
    void run_Success() throws Exception {
        // given
        ApplicationArguments args = mock(ApplicationArguments.class);
        given(photoBoothService.getAllPhotoBooths()).willReturn(Collections.emptyList());
        given(photoBoothService.getNearbyPhotoBooths(anyDouble(), anyDouble(), anyDouble()))
                .willReturn(Collections.emptyList());
        given(photoBoothService.getPhotoBoothsByBrand(anyString()))
                .willReturn(Collections.emptyList());

        // when
        cacheWarmupRunner.run(args);

        // then
        verify(photoBoothService).getAllPhotoBooths();
        // 8개 주요 지역 × getNearbyPhotoBooths
        verify(photoBoothService, times(8)).getNearbyPhotoBooths(anyDouble(), anyDouble(), eq(2.0));
        // 10개 주요 브랜드 × getPhotoBoothsByBrand
        verify(photoBoothService, times(10)).getPhotoBoothsByBrand(anyString());
    }

    @Test
    @DisplayName("캐시 워밍업 중 예외가 발생해도 앱 시작에 영향 없다")
    void run_Exception_DoesNotPreventStartup() throws Exception {
        // given
        ApplicationArguments args = mock(ApplicationArguments.class);
        given(photoBoothService.getAllPhotoBooths()).willThrow(new RuntimeException("DB unavailable"));

        // when & then - 예외가 전파되지 않아야 함
        cacheWarmupRunner.run(args);
    }
}
