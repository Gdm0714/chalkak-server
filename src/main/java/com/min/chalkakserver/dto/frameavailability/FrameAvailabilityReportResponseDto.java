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
public class FrameAvailabilityReportResponseDto {

    private Long frameCollabId;
    private Long photoBoothId;
    private String message;
    private LocalDateTime submittedAt;
}
