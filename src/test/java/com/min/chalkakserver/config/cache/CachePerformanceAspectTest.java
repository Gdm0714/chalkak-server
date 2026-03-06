package com.min.chalkakserver.config.cache;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.annotation.Cacheable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("CachePerformanceAspect 테스트")
class CachePerformanceAspectTest {

    @InjectMocks
    private CachePerformanceAspect cachePerformanceAspect;

    @Test
    @DisplayName("캐시 성능 측정이 정상적으로 동작한다 (캐시 히트 - 빠른 응답)")
    void measureCachePerformance_CacheHit() throws Throwable {
        // given
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Cacheable cacheable = mock(Cacheable.class);
        Signature signature = mock(Signature.class);

        given(joinPoint.proceed()).willReturn("cachedResult");
        given(joinPoint.getSignature()).willReturn(signature);
        given(signature.getName()).willReturn("getAllPhotoBooths");
        given(cacheable.value()).willReturn(new String[]{"photoBooths"});

        // when
        Object result = cachePerformanceAspect.measureCachePerformance(joinPoint, cacheable);

        // then
        assertThat(result).isEqualTo("cachedResult");
    }

    @Test
    @DisplayName("캐시 이름이 없으면 unknown으로 처리한다")
    void measureCachePerformance_EmptyCacheName() throws Throwable {
        // given
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Cacheable cacheable = mock(Cacheable.class);
        Signature signature = mock(Signature.class);

        given(joinPoint.proceed()).willReturn("result");
        given(joinPoint.getSignature()).willReturn(signature);
        given(signature.getName()).willReturn("someMethod");
        given(cacheable.value()).willReturn(new String[]{});

        // when
        Object result = cachePerformanceAspect.measureCachePerformance(joinPoint, cacheable);

        // then
        assertThat(result).isEqualTo("result");
    }

    @Test
    @DisplayName("원본 메서드에서 예외 발생 시 그대로 전파한다")
    void measureCachePerformance_ExceptionPropagation() throws Throwable {
        // given
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Cacheable cacheable = mock(Cacheable.class);

        given(joinPoint.proceed()).willThrow(new RuntimeException("DB connection failed"));

        // when & then
        assertThatThrownBy(() ->
                cachePerformanceAspect.measureCachePerformance(joinPoint, cacheable))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB connection failed");
    }
}
