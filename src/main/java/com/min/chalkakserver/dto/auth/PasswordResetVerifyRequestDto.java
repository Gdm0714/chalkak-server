package com.min.chalkakserver.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetVerifyRequestDto {

    @NotBlank(message = "이메일은 필수입니다")
    private String email;

    @NotBlank(message = "인증코드는 필수입니다")
    private String code;
}
