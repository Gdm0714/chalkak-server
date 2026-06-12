package com.min.chalkakserver.controller;

import com.min.chalkakserver.dto.frameavailability.FrameAvailabilityReportRequestDto;
import com.min.chalkakserver.dto.frameavailability.FrameAvailabilityReportResponseDto;
import com.min.chalkakserver.dto.frameavailability.FrameAvailabilityResponseDto;
import com.min.chalkakserver.security.CustomUserDetails;
import com.min.chalkakserver.service.FrameAvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "FrameAvailability", description = "프레임 재고 제보 API")
@RestController
@RequestMapping("/api/frame-availability")
@RequiredArgsConstructor
public class FrameAvailabilityController {

    private final FrameAvailabilityService frameAvailabilityService;

    @Operation(summary = "매장별 프레임 재고 조회", description = "특정 콜라보의 특정 매장 재고 상태를 최근 제보 기반으로 집계해 반환합니다.")
    @GetMapping("/collab/{frameCollabId}/booth/{photoBoothId}")
    public ResponseEntity<FrameAvailabilityResponseDto> getAvailability(
            @PathVariable Long frameCollabId,
            @PathVariable Long photoBoothId
    ) {
        return ResponseEntity.ok(frameAvailabilityService.getAvailability(frameCollabId, photoBoothId));
    }

    @Operation(summary = "콜라보 전체 매장 재고 조회", description = "콜라보의 모든 입고 매장 재고 상태를 한번에 조회합니다.")
    @GetMapping("/collab/{frameCollabId}")
    public ResponseEntity<List<FrameAvailabilityResponseDto>> getCollabAvailability(
            @PathVariable Long frameCollabId
    ) {
        return ResponseEntity.ok(frameAvailabilityService.getCollabAvailability(frameCollabId));
    }

    @Operation(summary = "프레임 재고 제보", description = "로그인 사용자가 매장의 프레임 재고 상태(있음/품절)를 제보합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/collab/{frameCollabId}/booth/{photoBoothId}")
    public ResponseEntity<FrameAvailabilityReportResponseDto> submitReport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long frameCollabId,
            @PathVariable Long photoBoothId,
            @Valid @RequestBody FrameAvailabilityReportRequestDto request
    ) {
        return ResponseEntity.ok(frameAvailabilityService.submitReport(
                userDetails.getId(), frameCollabId, photoBoothId, request));
    }
}
