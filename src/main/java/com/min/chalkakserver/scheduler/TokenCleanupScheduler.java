package com.min.chalkakserver.scheduler;

import com.min.chalkakserver.config.RateLimitConfig;
import com.min.chalkakserver.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 토큰 및 캐시 정리 스케줄러
 * 주기적으로 만료된 토큰과 불필요한 캐시를 정리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RateLimitConfig rateLimitConfig;

    /**
     * 만료된 Refresh Token 정리
     * 매일 새벽 3시에 실행
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredRefreshTokens() {
        log.info("Starting expired refresh token cleanup...");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            refreshTokenRepository.deleteAllExpiredTokens(now);
            log.info("Expired refresh tokens cleaned up successfully");
        } catch (Exception e) {
            log.error("Failed to cleanup expired refresh tokens: {}", e.getMessage());
        }
    }

    /**
     * 사용된 오래된 Refresh Token 정리 (Token Rotation으로 인한 사용된 토큰들)
     * 매일 새벽 4시에 실행
     */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanupUsedRefreshTokens() {
        log.info("Starting used refresh token cleanup...");
        
        try {
            // 24시간 이상 지난 사용된 토큰 삭제
            LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
            refreshTokenRepository.deleteUsedTokensOlderThan(cutoff);
            log.info("Used refresh tokens cleaned up successfully");
        } catch (Exception e) {
            log.error("Failed to cleanup used refresh tokens: {}", e.getMessage());
        }
    }

    /**
     * Rate Limit 버킷 캐시 정리
     * 매시간 실행
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupRateLimitBuckets() {
        log.info("Starting rate limit bucket cleanup...");
        
        try {
            rateLimitConfig.cleanupBuckets();
            log.info("Rate limit buckets cleaned up successfully");
        } catch (Exception e) {
            log.error("Failed to cleanup rate limit buckets: {}", e.getMessage());
        }
    }
}
