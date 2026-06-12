package com.min.chalkakserver.dto.framecollab;

import com.min.chalkakserver.entity.FrameCollab;
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
public class FrameCollabResponseDto {

    private Long id;
    private String title;
    private String brand;
    private String artistName;
    private String description;
    private String imageUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private FrameCollab.CollabStatus status;
    private int photoBoothCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FrameCollabResponseDto from(FrameCollab frameCollab, LocalDate today) {
        return FrameCollabResponseDto.builder()
                .id(frameCollab.getId())
                .title(frameCollab.getTitle())
                .brand(frameCollab.getBrand())
                .artistName(frameCollab.getArtistName())
                .description(frameCollab.getDescription())
                .imageUrl(frameCollab.getImageUrl())
                .startDate(frameCollab.getStartDate())
                .endDate(frameCollab.getEndDate())
                .status(frameCollab.getStatus(today))
                .photoBoothCount(frameCollab.getPhotoBooths().size())
                .createdAt(frameCollab.getCreatedAt())
                .updatedAt(frameCollab.getUpdatedAt())
                .build();
    }
}
