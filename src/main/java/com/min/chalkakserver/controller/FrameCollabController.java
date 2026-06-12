package com.min.chalkakserver.controller;

import com.min.chalkakserver.dto.PagedResponseDto;
import com.min.chalkakserver.dto.framecollab.FrameCollabDetailResponseDto;
import com.min.chalkakserver.dto.framecollab.FrameCollabResponseDto;
import com.min.chalkakserver.service.FrameCollabService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "FrameCollab", description = "한정판 프레임 콜라보 API")
@RestController
@RequestMapping("/api/frame-collabs")
@RequiredArgsConstructor
@Validated
public class FrameCollabController {

    private final FrameCollabService frameCollabService;

    @Operation(summary = "콜라보 피드 조회", description = "상태별 프레임 콜라보 목록을 조회합니다. (active: 진행중, upcoming: 예정, ended: 종료)")
    @GetMapping
    public ResponseEntity<PagedResponseDto<FrameCollabResponseDto>> getCollabs(
            @RequestParam(defaultValue = "active")
            @Pattern(regexp = "active|upcoming|ended", message = "status는 active, upcoming, ended 중 하나여야 합니다.")
            String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(frameCollabService.getCollabs(status, page, size));
    }

    @Operation(summary = "콜라보 상세 조회", description = "콜라보 상세 정보와 입고 매장 목록을 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<FrameCollabDetailResponseDto> getCollab(@PathVariable Long id) {
        return ResponseEntity.ok(frameCollabService.getCollab(id));
    }

    @Operation(summary = "매장별 콜라보 조회", description = "특정 사진관에서 진행 중이거나 예정된 콜라보 목록을 조회합니다.")
    @GetMapping("/photo-booth/{photoBoothId}")
    public ResponseEntity<List<FrameCollabResponseDto>> getCollabsByPhotoBooth(
            @PathVariable Long photoBoothId
    ) {
        return ResponseEntity.ok(frameCollabService.getCollabsByPhotoBooth(photoBoothId));
    }
}
