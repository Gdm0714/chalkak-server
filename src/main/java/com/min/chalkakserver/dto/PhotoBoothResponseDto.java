package com.min.chalkakserver.dto;

import com.min.chalkakserver.entity.PhotoBooth;
import java.time.LocalDateTime;

public class PhotoBoothResponseDto {
    
    private Long id;
    private String name;
    private String brand;
    private String address;
    private String roadAddress;
    private Double latitude;
    private Double longitude;
    private String operatingHours;
    private String phoneNumber;
    private String description;
    private String priceInfo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructor from Entity
    public PhotoBoothResponseDto(PhotoBooth photoBooth) {
        this.id = photoBooth.getId();
        this.name = photoBooth.getName();
        this.brand = photoBooth.getBrand();
        this.address = photoBooth.getAddress();
        this.roadAddress = photoBooth.getRoadAddress();
        this.latitude = photoBooth.getLatitude();
        this.longitude = photoBooth.getLongitude();
        this.operatingHours = photoBooth.getOperatingHours();
        this.phoneNumber = photoBooth.getPhoneNumber();
        this.description = photoBooth.getDescription();
        this.priceInfo = photoBooth.getPriceInfo();
        this.createdAt = photoBooth.getCreatedAt();
        this.updatedAt = photoBooth.getUpdatedAt();
    }
    
    // Default constructor
    public PhotoBoothResponseDto() {}
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getBrand() {
        return brand;
    }
    
    public void setBrand(String brand) {
        this.brand = brand;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getRoadAddress() {
        return roadAddress;
    }
    
    public void setRoadAddress(String roadAddress) {
        this.roadAddress = roadAddress;
    }
    
    public Double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
    
    public Double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
    
    public String getOperatingHours() {
        return operatingHours;
    }
    
    public void setOperatingHours(String operatingHours) {
        this.operatingHours = operatingHours;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getPriceInfo() {
        return priceInfo;
    }
    
    public void setPriceInfo(String priceInfo) {
        this.priceInfo = priceInfo;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
