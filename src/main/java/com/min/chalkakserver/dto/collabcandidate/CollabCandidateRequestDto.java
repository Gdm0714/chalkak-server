package com.min.chalkakserver.dto.collabcandidate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollabCandidateRequestDto {

    @Size(max = 500, message = "출처 URL은 500자 이하여야 합니다.")
    private String sourceUrl;

    @NotBlank(message = "공지 텍스트는 필수입니다.")
    private String rawText;
}
