package com.min.chalkakserver.dto.report;

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
public class ReportResponseDto {
    private Long id;
    private String name;
    private String brand;
    private String address;
    private String roadAddress;
    private Double latitude;
    private Double longitude;
    private String status;
    private String statusLabel;
    private Boolean isNearbyVerified;
    private Double distanceFromReport;
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    public static ReportResponseDto from(PhotoBoothReport report) {
        return ReportResponseDto.builder()
            .id(report.getId())
            .name(report.getName())
            .brand(report.getBrand())
            .address(report.getAddress())
            .roadAddress(report.getRoadAddress())
            .latitude(report.getLatitude())
            .longitude(report.getLongitude())
            .status(report.getStatus().name())
            .statusLabel(getStatusLabel(report.getStatus()))
            .isNearbyVerified(report.getIsNearbyVerified())
            .distanceFromReport(report.getDistanceFromReport())
            .adminNote(report.getAdminNote())
            .createdAt(report.getCreatedAt())
            .reviewedAt(report.getReviewedAt())
            .build();
    }

    private static String getStatusLabel(com.min.chalkakserver.entity.ReportStatus status) {
        return switch (status) {
            case PENDING -> "검토 대기";
            case REVIEWING -> "검토 중";
            case APPROVED -> "승인 완료";
            case REJECTED -> "거절";
            case DUPLICATE -> "중복 제보";
        };
    }
}
