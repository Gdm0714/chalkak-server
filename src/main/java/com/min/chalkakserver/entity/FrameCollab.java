package com.min.chalkakserver.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "frame_collabs",
    indexes = {
        @Index(name = "idx_collab_period", columnList = "start_date,end_date"),
        @Index(name = "idx_collab_brand", columnList = "brand"),
        @Index(name = "idx_collab_artist", columnList = "artist_name")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FrameCollab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 50)
    private String brand;

    @Column(name = "artist_name", length = 100)
    private String artistName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 피드 목록에서 콜라보별 매장 수(getPhotoBooths().size())를 읽을 때 N+1을 줄이기 위해 배치 로딩
    @ManyToMany(fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    @JoinTable(
        name = "frame_collab_booths",
        joinColumns = @JoinColumn(name = "frame_collab_id"),
        inverseJoinColumns = @JoinColumn(name = "photo_booth_id")
    )
    private Set<PhotoBooth> photoBooths = new HashSet<>();

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
    public FrameCollab(String title, String brand, String artistName, String description,
                       String imageUrl, LocalDate startDate, LocalDate endDate) {
        this.title = title;
        this.brand = brand;
        this.artistName = artistName;
        this.description = description;
        this.imageUrl = imageUrl;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void update(String title, String brand, String artistName, String description,
                       String imageUrl, LocalDate startDate, LocalDate endDate) {
        this.title = title;
        this.brand = brand;
        this.artistName = artistName;
        this.description = description;
        this.imageUrl = imageUrl;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void updatePhotoBooths(Set<PhotoBooth> newBooths) {
        this.photoBooths.clear();
        this.photoBooths.addAll(newBooths);
    }

    public CollabStatus getStatus(LocalDate today) {
        if (today.isBefore(startDate)) {
            return CollabStatus.UPCOMING;
        }
        if (today.isAfter(endDate)) {
            return CollabStatus.ENDED;
        }
        return CollabStatus.ACTIVE;
    }

    public enum CollabStatus {
        UPCOMING,
        ACTIVE,
        ENDED
    }
}
