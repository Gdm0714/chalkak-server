package com.min.chalkakserver.dto.collabcandidate;

import com.min.chalkakserver.entity.CollabCandidate;
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
public class CollabCandidateResponseDto {

    private Long id;
    private String sourceUrl;
    private String rawText;
    private String title;
    private String brand;
    private String artistName;
    private LocalDate startDate;
    private LocalDate endDate;
    private CollabCandidate.CandidateStatus status;
    private Long createdFrameCollabId;
    private LocalDateTime createdAt;

    public static CollabCandidateResponseDto from(CollabCandidate candidate) {
        return CollabCandidateResponseDto.builder()
                .id(candidate.getId())
                .sourceUrl(candidate.getSourceUrl())
                .rawText(candidate.getRawText())
                .title(candidate.getTitle())
                .brand(candidate.getBrand())
                .artistName(candidate.getArtistName())
                .startDate(candidate.getStartDate())
                .endDate(candidate.getEndDate())
                .status(candidate.getStatus())
                .createdFrameCollabId(candidate.getCreatedFrameCollabId())
                .createdAt(candidate.getCreatedAt())
                .build();
    }
}
