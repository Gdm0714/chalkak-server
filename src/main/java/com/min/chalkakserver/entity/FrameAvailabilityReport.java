package com.min.chalkakserver.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "frame_availability_reports",
    indexes = {
        @Index(name = "idx_availability_collab_booth_created_at",
               columnList = "frame_collab_id,photo_booth_id,created_at"),
        @Index(name = "idx_availability_user_collab_booth_created_at",
               columnList = "user_id,frame_collab_id,photo_booth_id,created_at")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FrameAvailabilityReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "frame_collab_id", nullable = false)
    private FrameCollab frameCollab;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_booth_id", nullable = false)
    private PhotoBooth photoBooth;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 20)
    private AvailabilityStatus availabilityStatus;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Builder
    public FrameAvailabilityReport(User user, FrameCollab frameCollab, PhotoBooth photoBooth,
                                   AvailabilityStatus availabilityStatus) {
        this.user = user;
        this.frameCollab = frameCollab;
        this.photoBooth = photoBooth;
        this.availabilityStatus = availabilityStatus;
    }

    public enum AvailabilityStatus {
        AVAILABLE(1),
        SOLD_OUT(0);

        private final int score;

        AvailabilityStatus(int score) {
            this.score = score;
        }

        public int getScore() {
            return score;
        }
    }
}
