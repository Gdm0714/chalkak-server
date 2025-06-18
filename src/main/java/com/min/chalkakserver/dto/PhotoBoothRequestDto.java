package com.min.chalkakserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PhotoBoothRequestDto {
    
    @NotBlank(message = "네컷사진관 이름은 필수입니다")
    private String name;
    
    private String brand;
    
    @NotBlank(message = "주소는 필수입니다")
    private String address;
    
    private String roadAddress;
    
    @NotNull(message = "위도는 필수입니다")
    private Double latitude;
    
    @NotNull(message = "경도는 필수입니다")
    private Double longitude;
    
    private String operatingHours;
    private String phoneNumber;
    private String description;
    private String priceInfo;
    
    // Default constructor
    public PhotoBoothRequestDto() {}
    
    // Constructor
    public PhotoBoothRequestDto(String name, String brand, String address, String roadAddress,
                               Double latitude, Double longitude, String operatingHours,
                               String phoneNumber, String description, String priceInfo) {
        this.name = name;
        this.brand = brand;
        this.address = address;
        this.roadAddress = roadAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.operatingHours = operatingHours;
        this.phoneNumber = phoneNumber;
        this.description = description;
        this.priceInfo = priceInfo;
    }
    
    // Getters and Setters
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
}
