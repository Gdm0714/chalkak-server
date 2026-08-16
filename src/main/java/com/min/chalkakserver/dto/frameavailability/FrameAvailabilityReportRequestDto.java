package com.min.chalkakserver.dto.frameavailability;

import com.min.chalkakserver.entity.FrameAvailabilityReport;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FrameAvailabilityReportRequestDto {

    @NotNull(message = "재고 상태는 필수입니다.")
    private FrameAvailabilityReport.AvailabilityStatus availabilityStatus;
}
