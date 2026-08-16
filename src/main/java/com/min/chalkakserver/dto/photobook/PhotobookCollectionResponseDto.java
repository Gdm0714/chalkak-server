package com.min.chalkakserver.dto.photobook;

import com.min.chalkakserver.entity.FrameCollab;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotobookCollectionResponseDto {

    private int totalCollabs;
    private int collectedCollabs;
    private long totalPhotos;
    private List<CollectionItemDto> items;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CollectionItemDto {

        private Long frameCollabId;
        private String title;
        private String brand;
        private String artistName;
        private String imageUrl;
        private LocalDate startDate;
        private LocalDate endDate;
        private FrameCollab.CollabStatus status;
        private long photoCount;
        private boolean collected;

        public static CollectionItemDto from(FrameCollab collab, LocalDate today, long photoCount) {
            return CollectionItemDto.builder()
                    .frameCollabId(collab.getId())
                    .title(collab.getTitle())
                    .brand(collab.getBrand())
                    .artistName(collab.getArtistName())
                    .imageUrl(collab.getImageUrl())
                    .startDate(collab.getStartDate())
                    .endDate(collab.getEndDate())
                    .status(collab.getStatus(today))
                    .photoCount(photoCount)
                    .collected(photoCount > 0)
                    .build();
        }
    }
}
