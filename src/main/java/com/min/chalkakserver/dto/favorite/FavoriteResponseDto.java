package com.min.chalkakserver.dto.favorite;

import com.min.chalkakserver.dto.PhotoBoothResponseDto;
import com.min.chalkakserver.entity.Favorite;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteResponseDto {
    private Long id;
    private PhotoBoothResponseDto photoBooth;
    private LocalDateTime createdAt;

    public static FavoriteResponseDto from(Favorite favorite) {
        return FavoriteResponseDto.builder()
            .id(favorite.getId())
            .photoBooth(PhotoBoothResponseDto.from(favorite.getPhotoBooth()))
            .createdAt(favorite.getCreatedAt())
            .build();
    }
}
