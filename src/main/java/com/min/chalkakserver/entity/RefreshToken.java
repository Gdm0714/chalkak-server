package com.min.chalkakserver.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens",
    indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_token", columnList = "token", unique = true),
        @Index(name = "idx_expires_at", columnList = "expires_at")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 토큰 사용 여부 (Token Rotation에서 재사용 감지용)
     * true면 이미 갱신에 사용된 토큰 (재사용 불가)
     */
    @Column(name = "is_used", nullable = false)
    private boolean used = false;

    /**
     * 토큰 패밀리 ID (같은 로그인 세션에서 발급된 토큰들을 그룹화)
     * 재사용 감지 시 같은 패밀리의 모든 토큰 무효화
     */
    @Column(name = "token_family", length = 36)
    private String tokenFamily;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Builder
    public RefreshToken(User user, String token, String deviceInfo, LocalDateTime expiresAt, String tokenFamily) {
        this.user = user;
        this.token = token;
        this.deviceInfo = deviceInfo;
        this.expiresAt = expiresAt;
        this.tokenFamily = tokenFamily;
        this.used = false;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 토큰을 사용됨으로 표시
     */
    public void markAsUsed() {
        this.used = true;
    }

    /**
     * 토큰 갱신 (새 토큰으로 교체)
     */
    public void updateToken(String newToken, LocalDateTime newExpiresAt) {
        this.token = newToken;
        this.expiresAt = newExpiresAt;
        this.used = false;  // 새 토큰은 사용되지 않은 상태
    }
}
