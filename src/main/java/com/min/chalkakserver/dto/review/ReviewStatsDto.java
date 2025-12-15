package com.min.chalkakserver.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStatsDto {
    private Long photoBoothId;
    private Double averageRating;
    private Long totalCount;
    private Map<Integer, Long> ratingDistribution;  // 별점별 리뷰 수 (1점~5점)
}
