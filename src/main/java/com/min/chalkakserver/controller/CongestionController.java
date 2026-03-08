package com.min.chalkakserver.controller;

import com.min.chalkakserver.dto.congestion.CongestionReportRequestDto;
import com.min.chalkakserver.dto.congestion.CongestionReportResponseDto;
import com.min.chalkakserver.dto.congestion.CongestionResponseDto;
import com.min.chalkakserver.security.CustomUserDetails;
import com.min.chalkakserver.service.CongestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Congestion", description = "혼잡도 API")
@RestController
@RequestMapping("/api/congestion")
@RequiredArgsConstructor
public class CongestionController {

    private final CongestionService congestionService;

    @Operation(summary = "현재 혼잡도 조회", description = "최근 제보 기반 혼잡도 집계를 반환합니다.")
    @GetMapping("/photo-booth/{photoBoothId}")
    public ResponseEntity<CongestionResponseDto> getCurrentCongestion(
            @PathVariable Long photoBoothId
    ) {
        return ResponseEntity.ok(congestionService.getCurrentCongestion(photoBoothId));
    }

    @Operation(summary = "일괄 혼잡도 조회", description = "여러 매장의 혼잡도를 한번에 조회합니다.")
    @GetMapping("/photo-booths/batch")
    public ResponseEntity<List<CongestionResponseDto>> getBatchCongestion(
            @RequestParam List<Long> ids
    ) {
        return ResponseEntity.ok(congestionService.getBatchCongestion(ids));
    }

    @Operation(summary = "혼잡도 제보", description = "로그인 사용자가 혼잡도를 제보합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/photo-booth/{photoBoothId}")
    public ResponseEntity<CongestionReportResponseDto> submitCongestionReport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long photoBoothId,
            @Valid @RequestBody CongestionReportRequestDto request
    ) {
        CongestionReportResponseDto response = congestionService.submitReport(
                userDetails.getId(),
                photoBoothId,
                request
        );
        return ResponseEntity.ok(response);
    }
}
