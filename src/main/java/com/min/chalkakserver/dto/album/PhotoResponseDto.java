package com.min.chalkakserver.dto.album;

import com.min.chalkakserver.entity.Photo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoResponseDto {
    private Long id;
    private String imageUrl;
    private String thumbnailUrl;
    private String memo;
    private boolean favorite;
    private Photo.PhotoSource source;
    private LocalDateTime photoDate;
    private LocalDateTime createdAt;

    public static PhotoResponseDto from(Photo photo) {
        return PhotoResponseDto.builder()
            .id(photo.getId())
            .imageUrl(photo.getImageUrl())
            .thumbnailUrl(photo.getThumbnailUrl())
            .memo(photo.getMemo())
            .favorite(photo.isFavorite())
            .source(photo.getSource())
            .photoDate(photo.getPhotoDate())
            .createdAt(photo.getCreatedAt())
            .build();
    }
}
