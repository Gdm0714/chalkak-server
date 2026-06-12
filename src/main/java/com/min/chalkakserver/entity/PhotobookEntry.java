package com.min.chalkakserver.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "photobook_entries",
    indexes = {
        @Index(name = "idx_photobook_user_created_at", columnList = "user_id,created_at"),
        @Index(name = "idx_photobook_user_collab", columnList = "user_id,frame_collab_id")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhotobookEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "frame_collab_id")
    private FrameCollab frameCollab;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_booth_id")
    private PhotoBooth photoBooth;

    @Column(name = "image_url", length = 500, nullable = false)
    private String imageUrl;

    @Column(name = "taken_at")
    private LocalDate takenAt;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder
    public PhotobookEntry(User user, FrameCollab frameCollab, PhotoBooth photoBooth,
                          String imageUrl, LocalDate takenAt, String memo) {
        this.user = user;
        this.frameCollab = frameCollab;
        this.photoBooth = photoBooth;
        this.imageUrl = imageUrl;
        this.takenAt = takenAt;
        this.memo = memo;
    }
}
