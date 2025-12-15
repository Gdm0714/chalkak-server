package com.min.chalkakserver.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialLoginRequestDto {

    @NotBlank(message = "Provider is required")
    @Pattern(regexp = "^(kakao|naver|apple)$", message = "Provider must be kakao, naver, or apple")
    private String provider;

    @NotBlank(message = "Access token is required")
    private String accessToken;

    private String deviceInfo;
}
