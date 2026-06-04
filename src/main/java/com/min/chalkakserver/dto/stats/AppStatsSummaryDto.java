package com.min.chalkakserver.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppStatsSummaryDto {
    private long photoBoothCount;
    private long activeUserCount;
    private long reviewCount;
}
