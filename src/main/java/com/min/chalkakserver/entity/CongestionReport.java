package com.min.chalkakserver.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "congestion_reports",
    indexes = {
        @Index(name = "idx_congestion_photo_booth_created_at", columnList = "photo_booth_id,created_at"),
        @Index(name = "idx_congestion_user_photo_booth_created_at", columnList = "user_id,photo_booth_id,created_at")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CongestionReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_booth_id", nullable = false)
    private PhotoBooth photoBooth;

    @Enumerated(EnumType.STRING)
    @Column(name = "congestion_level", nullable = false, length = 20)
    private CongestionLevel congestionLevel;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Builder
    public CongestionReport(User user, PhotoBooth photoBooth, CongestionLevel congestionLevel) {
        this.user = user;
        this.photoBooth = photoBooth;
        this.congestionLevel = congestionLevel;
    }

    public enum CongestionLevel {
        RELAXED(1),
        NORMAL(2),
        BUSY(3),
        VERY_BUSY(4),
        UNKNOWN(0);

        private final int score;

        CongestionLevel(int score) {
            this.score = score;
        }

        public int getScore() {
            return score;
        }
    }
}
