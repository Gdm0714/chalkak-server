package com.min.chalkakserver.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import java.time.LocalDateTime;

@Entity
@Table(name = "photo_booths", 
    indexes = {
        @Index(name = "idx_location", columnList = "latitude,longitude"),
        @Index(name = "idx_brand", columnList = "brand"),
        @Index(name = "idx_name", columnList = "name"),
        @Index(name = "idx_brand_series", columnList = "brand,series")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhotoBooth {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 50)
    private String brand;
    
    @Column(length = 50)
    private String series;
    
    @Column(length = 255)
    private String address;
    
    @Column(name = "road_address", length = 255)
    private String roadAddress;
    
    @Column(nullable = false)
    private Double latitude;
    
    @Column(nullable = false)
    private Double longitude;
    
    @Column(name = "operating_hours", length = 100)
    private String operatingHours;
    
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "price_info", length = 500)
    private String priceInfo;
    
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
    public PhotoBooth(String name, String brand, String series, String address, String roadAddress, 
                     Double latitude, Double longitude, String operatingHours, 
                     String phoneNumber, String description, String priceInfo) {
        this.name = name;
        this.brand = brand;
        this.series = series;
        this.address = address;
        this.roadAddress = roadAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.operatingHours = operatingHours;
        this.phoneNumber = phoneNumber;
        this.description = description;
        this.priceInfo = priceInfo;
    }
    
    public void update(String name, String brand, String series, String address, String roadAddress,
                      Double latitude, Double longitude, String operatingHours,
                      String phoneNumber, String description, String priceInfo) {
        this.name = name;
        this.brand = brand;
        this.series = series;
        this.address = address;
        this.roadAddress = roadAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.operatingHours = operatingHours;
        this.phoneNumber = phoneNumber;
        this.description = description;
        this.priceInfo = priceInfo;
    }
}
