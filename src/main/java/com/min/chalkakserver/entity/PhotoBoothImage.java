package com.min.chalkakserver.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "photo_booth_images",
    indexes = {
        @Index(name = "idx_photo_booth_image_booth_id", columnList = "photo_booth_id"),
        @Index(name = "idx_photo_booth_image_type", columnList = "image_type")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhotoBoothImage {

    public enum ImageType {
        EXTERIOR, INTERIOR, FRAME, SAMPLE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_booth_id", nullable = false)
    private PhotoBooth photoBooth;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false, length = 20)
    private ImageType imageType;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (displayOrder == null) {
            displayOrder = 0;
        }
    }

    @Builder
    public PhotoBoothImage(PhotoBooth photoBooth, String imageUrl, ImageType imageType, Integer displayOrder) {
        this.photoBooth = photoBooth;
        this.imageUrl = imageUrl;
        this.imageType = imageType;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
    }
}
