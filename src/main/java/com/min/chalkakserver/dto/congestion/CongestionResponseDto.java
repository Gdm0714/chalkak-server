package com.min.chalkakserver.dto.congestion;

import com.min.chalkakserver.entity.CongestionReport;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CongestionResponseDto {
    private Long photoBoothId;
    private CongestionReport.CongestionLevel congestionLevel;
    private ConfidenceLevel confidenceLevel;
    private Integer estimatedWaitMinutesMin;
    private Integer estimatedWaitMinutesMax;
    private Integer sampleSize;
    private LocalDateTime lastUpdatedAt;
    private String message;

    public enum ConfidenceLevel {
        LOW, MEDIUM, HIGH
    }
}
