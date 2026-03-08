package com.min.chalkakserver.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "photo_booth_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class PhotoBoothReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String brand;

    @Column(length = 50)
    private String series;

    @Column(nullable = false)
    private String address;

    private String roadAddress;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(length = 1000)
    private String description;

    @Column(length = 500)
    private String priceInfo;

    @Column(length = 1000)
    private String reporterName;

    private String reporterEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @Column(length = 500)
    private String adminNote;

    @Builder.Default
    private Boolean isNearbyVerified = false;

    private Double distanceFromReport;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum ReportStatus {
        PENDING, REVIEWING, APPROVED, REJECTED, DUPLICATE
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
