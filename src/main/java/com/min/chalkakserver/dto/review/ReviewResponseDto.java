package com.min.chalkakserver.dto.review;

import com.min.chalkakserver.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponseDto {
    private Long id;
    private Long photoBoothId;
    private String photoBoothName;
    private ReviewerDto reviewer;
    private Integer rating;
    private String content;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewerDto {
        private Long id;
        private String nickname;
        private String profileImageUrl;
    }

    public static ReviewResponseDto from(Review review) {
        return ReviewResponseDto.builder()
            .id(review.getId())
            .photoBoothId(review.getPhotoBooth().getId())
            .photoBoothName(review.getPhotoBooth().getName())
            .reviewer(ReviewerDto.builder()
                .id(review.getUser().getId())
                .nickname(review.getUser().getNickname())
                .profileImageUrl(review.getUser().getProfileImageUrl())
                .build())
            .rating(review.getRating())
            .content(review.getContent())
            .imageUrl(review.getImageUrl())
            .createdAt(review.getCreatedAt())
            .updatedAt(review.getUpdatedAt())
            .build();
    }
}
