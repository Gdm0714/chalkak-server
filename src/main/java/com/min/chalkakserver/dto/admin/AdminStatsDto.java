package com.min.chalkakserver.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsDto {
    private long totalPhotoBooths;
    private long totalUsers;
    private long totalReviews;
    private long totalFavorites;
    private long newUsersToday;
    private long newReviewsToday;
    private LocalDateTime generatedAt;
}
