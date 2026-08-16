package com.min.chalkakserver.dto.framecollab;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class FrameCollabRequestDto {

    @NotBlank(message = "콜라보 제목은 필수입니다.")
    @Size(max = 150, message = "콜라보 제목은 150자 이하여야 합니다.")
    private String title;

    @Size(max = 50, message = "브랜드명은 50자 이하여야 합니다.")
    private String brand;

    @Size(max = 100, message = "아티스트명은 100자 이하여야 합니다.")
    private String artistName;

    private String description;

    @Size(max = 500, message = "이미지 URL은 500자 이하여야 합니다.")
    private String imageUrl;

    @NotNull(message = "시작일은 필수입니다.")
    private LocalDate startDate;

    @NotNull(message = "종료일은 필수입니다.")
    private LocalDate endDate;

    private List<Long> photoBoothIds;

    @JsonIgnore
    @AssertTrue(message = "종료일은 시작일보다 빠를 수 없습니다.")
    public boolean isValidPeriod() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !endDate.isBefore(startDate);
    }
}
