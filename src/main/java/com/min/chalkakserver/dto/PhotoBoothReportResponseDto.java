package com.min.chalkakserver.dto;

import com.min.chalkakserver.entity.PhotoBoothReport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoBoothReportResponseDto {
    private Long id;
    private String name;
    private String brand;
    private String series;
    private String address;
    private String roadAddress;
    private Double latitude;
    private Double longitude;
    private String description;
    private String status;
    private String adminNote;
    private Boolean isNearbyVerified;
    private Double distanceFromReport;
    private LocalDateTime createdAt;

    public static PhotoBoothReportResponseDto from(PhotoBoothReport report) {
        return PhotoBoothReportResponseDto.builder()
            .id(report.getId())
            .name(report.getName())
            .brand(report.getBrand())
            .series(report.getSeries())
            .address(report.getAddress())
            .roadAddress(report.getRoadAddress())
            .latitude(report.getLatitude())
            .longitude(report.getLongitude())
            .description(report.getDescription())
            .status(report.getStatus().name())
            .adminNote(report.getAdminNote())
            .isNearbyVerified(report.getIsNearbyVerified())
            .distanceFromReport(report.getDistanceFromReport())
            .createdAt(report.getCreatedAt())
            .build();
    }
}
