package com.min.chalkakserver.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "photos",
    indexes = {
        @Index(name = "idx_photo_user_created", columnList = "user_id, created_at")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Photo {

    public enum PhotoSource {
        FILE_UPLOAD, QR_DOWNLOAD
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(length = 50)
    private String memo;

    @Column(name = "is_favorite")
    private boolean favorite;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PhotoSource source;

    @Column(name = "photo_date")
    private LocalDateTime photoDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (photoDate == null) {
            photoDate = createdAt;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder
    public Photo(User user, String imageUrl, String thumbnailUrl, String memo, boolean favorite, PhotoSource source, LocalDateTime photoDate) {
        this.user = user;
        this.imageUrl = imageUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.memo = memo;
        this.favorite = favorite;
        this.source = source;
        this.photoDate = photoDate;
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }

    public void updateFavorite(boolean favorite) {
        this.favorite = favorite;
    }
}
