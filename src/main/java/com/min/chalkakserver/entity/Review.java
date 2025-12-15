package com.min.chalkakserver.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews",
    indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_photo_booth_id", columnList = "photo_booth_id"),
        @Index(name = "idx_rating", columnList = "rating"),
        @Index(name = "idx_created_at", columnList = "created_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_photo_booth_review", columnNames = {"user_id", "photo_booth_id"})
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_booth_id", nullable = false)
    private PhotoBooth photoBooth;

    @Column(nullable = false)
    private Integer rating;  // 1-5점

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

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
    public Review(User user, PhotoBooth photoBooth, Integer rating, String content, String imageUrl) {
        this.user = user;
        this.photoBooth = photoBooth;
        this.rating = rating;
        this.content = content;
        this.imageUrl = imageUrl;
    }

    public void update(Integer rating, String content, String imageUrl) {
        if (rating != null) {
            this.rating = rating;
        }
        if (content != null) {
            this.content = content;
        }
        if (imageUrl != null) {
            this.imageUrl = imageUrl;
        }
    }
}
