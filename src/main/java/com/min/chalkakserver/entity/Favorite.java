package com.min.chalkakserver.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "favorites",
    indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_photo_booth_id", columnList = "photo_booth_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_photo_booth", columnNames = {"user_id", "photo_booth_id"})
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_booth_id", nullable = false)
    private PhotoBooth photoBooth;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Builder
    public Favorite(User user, PhotoBooth photoBooth) {
        this.user = user;
        this.photoBooth = photoBooth;
    }
}
