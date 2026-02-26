package com.min.chalkakserver.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsDto {
    private long reviewCount;
    private long favoriteCount;
    private Double averageRating;  // 내가 준 평균 평점
}
