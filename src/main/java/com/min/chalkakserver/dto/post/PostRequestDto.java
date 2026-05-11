package com.min.chalkakserver.dto.post;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostRequestDto {

    @NotBlank(message = "이미지 URL은 필수입니다.")
    private String imageUrl;

    private String caption;

    private Long photoBoothId;
}
