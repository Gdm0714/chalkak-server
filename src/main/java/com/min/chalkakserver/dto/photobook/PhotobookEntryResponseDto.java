package com.min.chalkakserver.dto.photobook;

import com.min.chalkakserver.entity.PhotobookEntry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotobookEntryResponseDto {

    private Long id;
    private String imageUrl;
    private Long frameCollabId;
    private String frameCollabTitle;
    private String frameCollabArtistName;
    private Long photoBoothId;
    private String photoBoothName;
    private LocalDate takenAt;
    private String memo;
    private LocalDateTime createdAt;

    public static PhotobookEntryResponseDto from(PhotobookEntry entry) {
        return PhotobookEntryResponseDto.builder()
                .id(entry.getId())
                .imageUrl(entry.getImageUrl())
                .frameCollabId(entry.getFrameCollab() != null ? entry.getFrameCollab().getId() : null)
                .frameCollabTitle(entry.getFrameCollab() != null ? entry.getFrameCollab().getTitle() : null)
                .frameCollabArtistName(entry.getFrameCollab() != null ? entry.getFrameCollab().getArtistName() : null)
                .photoBoothId(entry.getPhotoBooth() != null ? entry.getPhotoBooth().getId() : null)
                .photoBoothName(entry.getPhotoBooth() != null ? entry.getPhotoBooth().getName() : null)
                .takenAt(entry.getTakenAt())
                .memo(entry.getMemo())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
