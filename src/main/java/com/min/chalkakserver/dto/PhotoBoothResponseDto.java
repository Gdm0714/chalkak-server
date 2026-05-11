package com.min.chalkakserver.dto;

import com.min.chalkakserver.entity.PhotoBooth;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
    private List<PhotoBoothImageDto> images;
    private List<String> tags;

    public static PhotoBoothResponseDto from(PhotoBooth photoBooth) {
        List<String> tagNames = photoBooth.getTags() != null
                ? photoBooth.getTags().stream().map(t -> t.getName()).collect(Collectors.toList())
                : List.of();
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
                .images(List.of())
                .tags(tagNames)
                .build();
    }

    public static PhotoBoothResponseDto from(PhotoBooth photoBooth, List<PhotoBoothImageDto> images) {
        List<String> tagNames = photoBooth.getTags() != null
                ? photoBooth.getTags().stream().map(t -> t.getName()).collect(Collectors.toList())
                : List.of();
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
                .images(images)
                .tags(tagNames)
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
        this.images = List.of();
        this.tags = List.of();
    }
}
