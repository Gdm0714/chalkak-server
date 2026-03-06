package com.min.chalkakserver.dto.congestion;

import com.min.chalkakserver.entity.CongestionReport;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CongestionReportRequestDto {

    @NotNull(message = "혼잡도 레벨은 필수입니다")
    private CongestionReport.CongestionLevel congestionLevel;
}
