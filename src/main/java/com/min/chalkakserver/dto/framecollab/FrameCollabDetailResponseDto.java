package com.min.chalkakserver.dto.framecollab;

import com.min.chalkakserver.dto.PhotoBoothResponseDto;
import com.min.chalkakserver.entity.FrameCollab;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FrameCollabDetailResponseDto {

    private Long id;
    private String title;
    private String brand;
    private String artistName;
    private String description;
    private String imageUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private FrameCollab.CollabStatus status;
    private List<PhotoBoothResponseDto> photoBooths;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FrameCollabDetailResponseDto from(FrameCollab frameCollab, LocalDate today) {
        List<PhotoBoothResponseDto> booths = frameCollab.getPhotoBooths().stream()
                .sorted(Comparator.comparing(pb -> pb.getId()))
                .map(PhotoBoothResponseDto::from)
                .toList();
        return FrameCollabDetailResponseDto.builder()
                .id(frameCollab.getId())
                .title(frameCollab.getTitle())
                .brand(frameCollab.getBrand())
                .artistName(frameCollab.getArtistName())
                .description(frameCollab.getDescription())
                .imageUrl(frameCollab.getImageUrl())
                .startDate(frameCollab.getStartDate())
                .endDate(frameCollab.getEndDate())
                .status(frameCollab.getStatus(today))
                .photoBooths(booths)
                .createdAt(frameCollab.getCreatedAt())
                .updatedAt(frameCollab.getUpdatedAt())
                .build();
    }
}
