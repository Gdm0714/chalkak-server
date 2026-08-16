package com.min.chalkakserver.dto.collabcandidate;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 후보 승인 시 파싱된 초안을 덮어쓸 수 있는 선택 필드들.
 * null인 필드는 후보의 파싱 값을 그대로 사용한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollabCandidateApproveRequestDto {

    @Size(max = 150, message = "콜라보 제목은 150자 이하여야 합니다.")
    private String title;

    @Size(max = 50, message = "브랜드명은 50자 이하여야 합니다.")
    private String brand;

    @Size(max = 100, message = "아티스트명은 100자 이하여야 합니다.")
    private String artistName;

    private String description;

    @Size(max = 500, message = "이미지 URL은 500자 이하여야 합니다.")
    private String imageUrl;

    private LocalDate startDate;

    private LocalDate endDate;

    private List<Long> photoBoothIds;
}
