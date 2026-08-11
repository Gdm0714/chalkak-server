package com.min.chalkakserver.dto.auth;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateRequestDto {
    
    @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하여야 합니다")
    private String nickname;

    /**
     * 업로드된 프로필 이미지 URL (`POST /api/uploads/image` 응답값).
     * null이면 기존 이미지를 유지한다.
     */
    @Size(max = 500, message = "프로필 이미지 URL은 500자를 초과할 수 없습니다")
    @Pattern(regexp = "^https?://.+", message = "프로필 이미지 URL 형식이 올바르지 않습니다")
    private String profileImageUrl;
}
