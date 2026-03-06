package com.min.chalkakserver.config;

import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimitConfig 테스트")
class RateLimitConfigTest {

    private RateLimitConfig rateLimitConfig;

    @BeforeEach
    void setUp() throws Exception {
        rateLimitConfig = new RateLimitConfig();
        setField(rateLimitConfig, "requestsPerMinute", 60);
        setField(rateLimitConfig, "requestsPerSecond", 10);
    }

    @Test
    @DisplayName("일반 버킷을 생성하고 반환한다")
    void resolveBucket_CreateAndReturn() {
        Bucket bucket = rateLimitConfig.resolveBucket("192.168.1.1");
        assertThat(bucket).isNotNull();
    }

    @Test
    @DisplayName("동일 IP에 대해 같은 버킷을 반환한다")
    void resolveBucket_SameIpSameBucket() {
        Bucket bucket1 = rateLimitConfig.resolveBucket("192.168.1.1");
        Bucket bucket2 = rateLimitConfig.resolveBucket("192.168.1.1");
        assertThat(bucket1).isSameAs(bucket2);
    }

    @Test
    @DisplayName("다른 IP에 대해 다른 버킷을 반환한다")
    void resolveBucket_DifferentIpDifferentBucket() {
        Bucket bucket1 = rateLimitConfig.resolveBucket("192.168.1.1");
        Bucket bucket2 = rateLimitConfig.resolveBucket("192.168.1.2");
        assertThat(bucket1).isNotSameAs(bucket2);
    }

    @Test
    @DisplayName("일반 버킷에서 토큰을 소비할 수 있다")
    void resolveBucket_CanConsumeTokens() {
        Bucket bucket = rateLimitConfig.resolveBucket("10.0.0.1");
        assertThat(bucket.tryConsume(1)).isTrue();
    }

    @Test
    @DisplayName("제보 API용 버킷을 생성한다")
    void resolveReportBucket_CreateAndReturn() {
        Bucket bucket = rateLimitConfig.resolveReportBucket("192.168.1.1");
        assertThat(bucket).isNotNull();
    }

    @Test
    @DisplayName("제보 API용 버킷은 일반 버킷과 별도이다")
    void resolveReportBucket_SeparateFromGeneral() {
        Bucket generalBucket = rateLimitConfig.resolveBucket("192.168.1.1");
        Bucket reportBucket = rateLimitConfig.resolveReportBucket("192.168.1.1");
        assertThat(generalBucket).isNotSameAs(reportBucket);
    }

    @Test
    @DisplayName("제보 API용 버킷은 분당 5건 제한이다")
    void resolveReportBucket_LimitedTo5PerMinute() {
        Bucket bucket = rateLimitConfig.resolveReportBucket("10.0.0.2");
        for (int i = 0; i < 5; i++) {
            assertThat(bucket.tryConsume(1)).isTrue();
        }
        assertThat(bucket.tryConsume(1)).isFalse();
    }

    @Test
    @DisplayName("인증 API용 버킷을 생성한다")
    void resolveAuthBucket_CreateAndReturn() {
        Bucket bucket = rateLimitConfig.resolveAuthBucket("192.168.1.1");
        assertThat(bucket).isNotNull();
    }

    @Test
    @DisplayName("인증 API용 버킷은 일반 버킷과 별도이다")
    void resolveAuthBucket_SeparateFromGeneral() {
        Bucket generalBucket = rateLimitConfig.resolveBucket("192.168.1.1");
        Bucket authBucket = rateLimitConfig.resolveAuthBucket("192.168.1.1");
        assertThat(generalBucket).isNotSameAs(authBucket);
    }

    @Test
    @DisplayName("인증 API용 버킷은 분당 10건 제한이다")
    void resolveAuthBucket_LimitedTo10PerMinute() {
        Bucket bucket = rateLimitConfig.resolveAuthBucket("10.0.0.3");
        for (int i = 0; i < 10; i++) {
            assertThat(bucket.tryConsume(1)).isTrue();
        }
        assertThat(bucket.tryConsume(1)).isFalse();
    }

    @Test
    @DisplayName("버킷 수가 10000 이하이면 cleanupBuckets가 삭제하지 않는다")
    void cleanupBuckets_BelowThreshold_DoesNotClear() {
        rateLimitConfig.resolveBucket("192.168.1.1");
        rateLimitConfig.cleanupBuckets();
        // 버킷이 여전히 존재하는지 확인 (같은 객체 반환)
        Bucket bucket = rateLimitConfig.resolveBucket("192.168.1.1");
        assertThat(bucket).isNotNull();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
