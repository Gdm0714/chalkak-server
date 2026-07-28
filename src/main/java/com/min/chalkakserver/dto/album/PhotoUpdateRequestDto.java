package com.min.chalkakserver.dto.album;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoUpdateRequestDto {

    @Size(max = 50, message = "메모는 50자를 초과할 수 없습니다.")
    private String memo;

    private Boolean favorite;
}
