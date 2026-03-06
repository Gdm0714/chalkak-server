package com.min.chalkakserver.scheduler;

import com.min.chalkakserver.config.RateLimitConfig;
import com.min.chalkakserver.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenCleanupSchedulerTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RateLimitConfig rateLimitConfig;

    @InjectMocks
    private TokenCleanupScheduler scheduler;

    // ==================== cleanupExpiredRefreshTokens ====================

    @Test
    void cleanupExpiredRefreshTokens_Success_ShouldCallRepository() {
        // Given
        doNothing().when(refreshTokenRepository).deleteAllExpiredTokens(any());

        // When
        scheduler.cleanupExpiredRefreshTokens();

        // Then
        verify(refreshTokenRepository, times(1)).deleteAllExpiredTokens(any());
    }

    @Test
    void cleanupExpiredRefreshTokens_RepositoryThrows_ShouldNotPropagateException() {
        // Given
        doThrow(new RuntimeException("DB connection failed"))
                .when(refreshTokenRepository).deleteAllExpiredTokens(any());

        // When & Then
        assertDoesNotThrow(() -> scheduler.cleanupExpiredRefreshTokens());
        verify(refreshTokenRepository, times(1)).deleteAllExpiredTokens(any());
    }

    // ==================== cleanupUsedRefreshTokens ====================

    @Test
    void cleanupUsedRefreshTokens_Success_ShouldCallRepository() {
        // Given
        doNothing().when(refreshTokenRepository).deleteUsedTokensOlderThan(any());

        // When
        scheduler.cleanupUsedRefreshTokens();

        // Then
        verify(refreshTokenRepository, times(1)).deleteUsedTokensOlderThan(any());
    }

    @Test
    void cleanupUsedRefreshTokens_RepositoryThrows_ShouldNotPropagateException() {
        // Given
        doThrow(new RuntimeException("Timeout"))
                .when(refreshTokenRepository).deleteUsedTokensOlderThan(any());

        // When & Then
        assertDoesNotThrow(() -> scheduler.cleanupUsedRefreshTokens());
        verify(refreshTokenRepository, times(1)).deleteUsedTokensOlderThan(any());
    }

    // ==================== cleanupRateLimitBuckets ====================

    @Test
    void cleanupRateLimitBuckets_Success_ShouldCallRateLimitConfig() {
        // Given
        doNothing().when(rateLimitConfig).cleanupBuckets();

        // When
        scheduler.cleanupRateLimitBuckets();

        // Then
        verify(rateLimitConfig, times(1)).cleanupBuckets();
    }

    @Test
    void cleanupRateLimitBuckets_ConfigThrows_ShouldNotPropagateException() {
        // Given
        doThrow(new RuntimeException("Bucket cleanup failed"))
                .when(rateLimitConfig).cleanupBuckets();

        // When & Then
        assertDoesNotThrow(() -> scheduler.cleanupRateLimitBuckets());
        verify(rateLimitConfig, times(1)).cleanupBuckets();
    }
}
