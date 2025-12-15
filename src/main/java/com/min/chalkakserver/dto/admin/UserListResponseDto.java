package com.min.chalkakserver.dto.admin;

import com.min.chalkakserver.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserListResponseDto {
    private Long id;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String provider;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private long reviewCount;
    private long favoriteCount;

    public static UserListResponseDto from(User user, long reviewCount, long favoriteCount) {
        return UserListResponseDto.builder()
            .id(user.getId())
            .email(user.getEmail())
            .nickname(user.getNickname())
            .profileImageUrl(user.getProfileImageUrl())
            .provider(user.getProvider().name())
            .role(user.getRole().name())
            .createdAt(user.getCreatedAt())
            .lastLoginAt(user.getLastLoginAt())
            .reviewCount(reviewCount)
            .favoriteCount(favoriteCount)
            .build();
    }
}
