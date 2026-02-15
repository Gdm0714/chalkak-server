package com.min.chalkakserver.dto.congestion;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CongestionReportResponseDto {
    private Long photoBoothId;
    private String message;
    private LocalDateTime submittedAt;
}
