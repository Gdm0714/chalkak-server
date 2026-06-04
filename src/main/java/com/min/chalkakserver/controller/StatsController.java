package com.min.chalkakserver.controller;

import com.min.chalkakserver.dto.stats.AppStatsSummaryDto;
import com.min.chalkakserver.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/summary")
    public ResponseEntity<AppStatsSummaryDto> getSummary() {
        return ResponseEntity.ok(statsService.getSummary());
    }
}
