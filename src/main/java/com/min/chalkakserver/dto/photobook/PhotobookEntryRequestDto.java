package com.min.chalkakserver.dto.photobook;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotobookEntryRequestDto {

    @NotBlank(message = "이미지 URL은 필수입니다.")
    @Size(max = 500, message = "이미지 URL은 500자 이하여야 합니다.")
    private String imageUrl;

    private Long frameCollabId;

    private Long photoBoothId;

    private LocalDate takenAt;

    @Size(max = 1000, message = "메모는 1000자 이하여야 합니다.")
    private String memo;
}
