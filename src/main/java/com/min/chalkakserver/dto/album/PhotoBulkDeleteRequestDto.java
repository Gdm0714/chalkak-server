package com.min.chalkakserver.dto.album;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoBulkDeleteRequestDto {

    @NotEmpty(message = "삭제할 사진 ID 목록이 비어있습니다.")
    private List<Long> ids;
}
