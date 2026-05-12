package com.min.chalkakserver.controller;

import com.min.chalkakserver.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/summary")
    @Operation(summary = "앱 공개 통계", description = "앱 정보 화면에 표시할 공개 요약 통계를 조회합니다")
    public ResponseEntity<Map<String, Long>> getSummary() {
        return ResponseEntity.ok(statsService.getSummary());
    }
}
