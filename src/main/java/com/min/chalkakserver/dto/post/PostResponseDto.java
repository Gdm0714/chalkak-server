package com.min.chalkakserver.dto.post;

import com.min.chalkakserver.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponseDto {

    private Long id;
    private String userNickname;
    private String userProfileImageUrl;
    private Long photoBoothId;
    private String photoBoothName;
    private String imageUrl;
    private String caption;
    private int likesCount;
    private boolean isLiked;
    private LocalDateTime createdAt;

    public static PostResponseDto from(Post post, boolean isLiked) {
        return PostResponseDto.builder()
            .id(post.getId())
            .userNickname(post.getUser().getNickname())
            .userProfileImageUrl(post.getUser().getProfileImageUrl())
            .photoBoothId(post.getPhotoBooth() != null ? post.getPhotoBooth().getId() : null)
            .photoBoothName(post.getPhotoBooth() != null ? post.getPhotoBooth().getName() : null)
            .imageUrl(post.getImageUrl())
            .caption(post.getCaption())
            .likesCount(post.getLikesCount())
            .isLiked(isLiked)
            .createdAt(post.getCreatedAt())
            .build();
    }
}
