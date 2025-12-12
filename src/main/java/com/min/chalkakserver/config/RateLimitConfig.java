package com.min.chalkakserver.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting 설정
 * IP 기반으로 요청 횟수를 제한합니다.
 */
@Configuration
public class RateLimitConfig {

    // IP별 버킷 저장소
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Value("${rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    @Value("${rate-limit.requests-per-second:10}")
    private int requestsPerSecond;

    /**
     * IP 주소에 대한 버킷 가져오기 (없으면 생성)
     */
    public Bucket resolveBucket(String ipAddress) {
        return buckets.computeIfAbsent(ipAddress, this::createNewBucket);
    }

    /**
     * 새 버킷 생성
     * - 초당 10개 요청 허용
     * - 분당 60개 요청 허용
     */
    private Bucket createNewBucket(String ipAddress) {
        Bandwidth perSecondLimit = Bandwidth.classic(
                requestsPerSecond,
                Refill.greedy(requestsPerSecond, Duration.ofSeconds(1))
        );

        Bandwidth perMinuteLimit = Bandwidth.classic(
                requestsPerMinute,
                Refill.greedy(requestsPerMinute, Duration.ofMinutes(1))
        );

        return Bucket.builder()
                .addLimit(perSecondLimit)
                .addLimit(perMinuteLimit)
                .build();
    }

    /**
     * 제보 API용 더 엄격한 버킷 (스팸 방지)
     * - 분당 5개 요청만 허용
     */
    public Bucket resolveReportBucket(String ipAddress) {
        String key = "report_" + ipAddress;
        return buckets.computeIfAbsent(key, k -> createReportBucket());
    }

    private Bucket createReportBucket() {
        Bandwidth perMinuteLimit = Bandwidth.classic(
                5,
                Refill.greedy(5, Duration.ofMinutes(1))
        );

        return Bucket.builder()
                .addLimit(perMinuteLimit)
                .build();
    }

    /**
     * 버킷 캐시 정리 (메모리 관리)
     * 주기적으로 호출하여 오래된 버킷 제거
     */
    public void cleanupBuckets() {
        // 간단한 구현: 모든 버킷 제거 (다음 요청 시 새로 생성)
        // 프로덕션에서는 LRU 캐시 사용 권장
        if (buckets.size() > 10000) {
            buckets.clear();
        }
    }
}
