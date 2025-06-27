package com.min.chalkakserver.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "photo_booths", 
    indexes = {
        @Index(name = "idx_location", columnList = "latitude,longitude"),
        @Index(name = "idx_brand", columnList = "brand"),
        @Index(name = "idx_name", columnList = "name")
    })
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoBooth {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 50)
    private String brand;
    
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
}
