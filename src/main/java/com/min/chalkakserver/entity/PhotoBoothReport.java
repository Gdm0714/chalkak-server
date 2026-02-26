package com.min.chalkakserver.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "photo_booth_reports",
    indexes = {
        @Index(name = "idx_report_user_id", columnList = "user_id"),
        @Index(name = "idx_report_status", columnList = "status"),
        @Index(name = "idx_report_location", columnList = "latitude,longitude"),
        @Index(name = "idx_report_created_at", columnList = "created_at")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhotoBoothReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;  // nullable - 비로그인 제보도 가능

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String brand;

    @Column(length = 50)
    private String series;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(name = "road_address", length = 255)
    private String roadAddress;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "price_info", length = 500)
    private String priceInfo;

    @Column(name = "reporter_name", length = 100)
    private String reporterName;

    @Column(name = "reporter_email", length = 255)
    private String reporterEmail;

    // GPS 검증 관련
    @Column(name = "user_latitude")
    private Double userLatitude;  // 제보 시 사용자의 실제 GPS 위도

    @Column(name = "user_longitude")
    private Double userLongitude;  // 제보 시 사용자의 실제 GPS 경도

    @Column(name = "distance_from_report")
    private Double distanceFromReport;  // 사용자 위치와 제보 위치 간 거리 (km)

    @Column(name = "is_nearby_verified")
    private Boolean isNearbyVerified;  // 사용자가 근처에서 제보했는지 여부

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;  // 관리자 메모

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = ReportStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder
    public PhotoBoothReport(User user, String name, String brand, String series,
                           String address, String roadAddress, Double latitude, Double longitude,
                           String description, String priceInfo, String reporterName, String reporterEmail,
                           Double userLatitude, Double userLongitude, Double distanceFromReport,
                           Boolean isNearbyVerified, ReportStatus status) {
        this.user = user;
        this.name = name;
        this.brand = brand;
        this.series = series;
        this.address = address;
        this.roadAddress = roadAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
        this.priceInfo = priceInfo;
        this.reporterName = reporterName;
        this.reporterEmail = reporterEmail;
        this.userLatitude = userLatitude;
        this.userLongitude = userLongitude;
        this.distanceFromReport = distanceFromReport;
        this.isNearbyVerified = isNearbyVerified;
        this.status = status != null ? status : ReportStatus.PENDING;
    }

    public void updateStatus(ReportStatus status, String adminNote) {
        this.status = status;
        this.adminNote = adminNote;
        this.reviewedAt = LocalDateTime.now();
    }
}
