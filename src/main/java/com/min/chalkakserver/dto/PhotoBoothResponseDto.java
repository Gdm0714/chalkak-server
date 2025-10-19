package com.min.chalkakserver.dto;

import com.min.chalkakserver.entity.PhotoBooth;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoBoothResponseDto {
    
    private Long id;
    private String name;
    private String brand;
    private String series;
    private String address;
    private String roadAddress;
    private Double latitude;
    private Double longitude;
    private String description;
    private String priceInfo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static PhotoBoothResponseDto from(PhotoBooth photoBooth) {
        return PhotoBoothResponseDto.builder()
                .id(photoBooth.getId())
                .name(photoBooth.getName())
                .brand(photoBooth.getBrand())
                .series(photoBooth.getSeries())
                .address(photoBooth.getAddress())
                .roadAddress(photoBooth.getRoadAddress())
                .latitude(photoBooth.getLatitude())
                .longitude(photoBooth.getLongitude())
                .description(photoBooth.getDescription())
                .priceInfo(photoBooth.getPriceInfo())
                .createdAt(photoBooth.getCreatedAt())
                .updatedAt(photoBooth.getUpdatedAt())
                .build();
    }
    
    @Deprecated
    public PhotoBoothResponseDto(PhotoBooth photoBooth) {
        this.id = photoBooth.getId();
        this.name = photoBooth.getName();
        this.brand = photoBooth.getBrand();
        this.series = photoBooth.getSeries();
        this.address = photoBooth.getAddress();
        this.roadAddress = photoBooth.getRoadAddress();
        this.latitude = photoBooth.getLatitude();
        this.longitude = photoBooth.getLongitude();
        this.description = photoBooth.getDescription();
        this.priceInfo = photoBooth.getPriceInfo();
        this.createdAt = photoBooth.getCreatedAt();
        this.updatedAt = photoBooth.getUpdatedAt();
    }
}
