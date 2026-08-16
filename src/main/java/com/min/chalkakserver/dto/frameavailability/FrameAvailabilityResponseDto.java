package com.min.chalkakserver.dto.frameavailability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FrameAvailabilityResponseDto {

    private Long frameCollabId;
    private Long photoBoothId;
    private ResultStatus status;
    private ConfidenceLevel confidenceLevel;
    private int sampleSize;
    private LocalDateTime lastUpdatedAt;
    private String message;

    public enum ResultStatus {
        AVAILABLE,
        SOLD_OUT,
        UNKNOWN
    }

    public enum ConfidenceLevel {
        LOW,
        MEDIUM,
        HIGH
    }
}
