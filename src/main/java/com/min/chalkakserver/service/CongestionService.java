package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.congestion.CongestionReportRequestDto;
import com.min.chalkakserver.dto.congestion.CongestionReportResponseDto;
import com.min.chalkakserver.dto.congestion.CongestionResponseDto;
import com.min.chalkakserver.entity.CongestionReport;
import com.min.chalkakserver.entity.PhotoBooth;
import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.exception.DuplicateCongestionReportException;
import com.min.chalkakserver.exception.PhotoBoothNotFoundException;
import com.min.chalkakserver.repository.CongestionReportRepository;
import com.min.chalkakserver.repository.PhotoBoothRepository;
import com.min.chalkakserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CongestionService {

    private static final int AGGREGATION_WINDOW_MINUTES = 60;
    private static final int REPORT_COOLDOWN_MINUTES = 60;
    private static final double MAX_REPORT_DISTANCE_METERS = 500.0;

    private final CongestionReportRepository congestionReportRepository;
    private final PhotoBoothRepository photoBoothRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CongestionResponseDto getCurrentCongestion(Long photoBoothId) {
        PhotoBooth photoBooth = photoBoothRepository.findById(photoBoothId)
                .orElseThrow(() -> new PhotoBoothNotFoundException(photoBoothId));

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(AGGREGATION_WINDOW_MINUTES);
        List<CongestionReport> recentReports =
                congestionReportRepository.findByPhotoBoothAndCreatedAtAfterOrderByCreatedAtDesc(photoBooth, cutoff);

        if (recentReports.isEmpty()) {
            return CongestionResponseDto.builder()
                    .photoBoothId(photoBoothId)
                    .congestionLevel(CongestionReport.CongestionLevel.UNKNOWN)
                    .confidenceLevel(CongestionResponseDto.ConfidenceLevel.LOW)
                    .sampleSize(0)
                    .message("아직 혼잡도 데이터가 부족해요.")
                    .build();
        }

        double weightedScore = calculateWeightedScore(recentReports);
        CongestionReport.CongestionLevel level = mapScoreToLevel(weightedScore);

        int[] waitRange = mapLevelToWaitRange(level);
        CongestionResponseDto.ConfidenceLevel confidence = mapConfidence(recentReports.size());

        return CongestionResponseDto.builder()
                .photoBoothId(photoBoothId)
                .congestionLevel(level)
                .confidenceLevel(confidence)
                .estimatedWaitMinutesMin(waitRange[0])
                .estimatedWaitMinutesMax(waitRange[1])
                .sampleSize(recentReports.size())
                .lastUpdatedAt(recentReports.get(0).getCreatedAt())
                .message(buildMessage(level, confidence, recentReports.size()))
                .build();
    }

    @Transactional(readOnly = true)
    public List<CongestionResponseDto> getBatchCongestion(List<Long> photoBoothIds) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(AGGREGATION_WINDOW_MINUTES);

        return photoBoothIds.stream().map(photoBoothId -> {
            try {
                return getCurrentCongestion(photoBoothId);
            } catch (Exception e) {
                log.warn("Failed to get congestion for photoBoothId={}: {}", photoBoothId, e.getMessage());
                return CongestionResponseDto.builder()
                        .photoBoothId(photoBoothId)
                        .congestionLevel(CongestionReport.CongestionLevel.UNKNOWN)
                        .confidenceLevel(CongestionResponseDto.ConfidenceLevel.LOW)
                        .sampleSize(0)
                        .message("혼잡도 정보를 불러올 수 없습니다.")
                        .build();
            }
        }).toList();
    }

    public CongestionReportResponseDto submitReport(Long userId, Long photoBoothId, CongestionReportRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        PhotoBooth photoBooth = photoBoothRepository.findById(photoBoothId)
                .orElseThrow(() -> new PhotoBoothNotFoundException(photoBoothId));

        double distance = calculateDistanceInMeters(
                request.getLatitude(), request.getLongitude(),
                photoBooth.getLatitude(), photoBooth.getLongitude()
        );
        if (distance > MAX_REPORT_DISTANCE_METERS) {
            throw new IllegalArgumentException(
                    String.format("매장에서 너무 멀리 있습니다. (%.0fm) 매장 근처에서 제보해주세요.", distance)
            );
        }

        LocalDateTime cooldownCutoff = LocalDateTime.now().minusMinutes(REPORT_COOLDOWN_MINUTES);
        boolean alreadyReported = congestionReportRepository.existsByUserAndPhotoBoothAndCreatedAtAfter(
                user, photoBooth, cooldownCutoff
        );

        if (alreadyReported) {
            throw new DuplicateCongestionReportException(photoBoothId);
        }

        CongestionReport report = CongestionReport.builder()
                .user(user)
                .photoBooth(photoBooth)
                .congestionLevel(request.getCongestionLevel())
                .build();

        CongestionReport saved = congestionReportRepository.save(report);
        log.info("Congestion report submitted: userId={}, photoBoothId={}, level={}",
                userId, photoBoothId, request.getCongestionLevel());

        return CongestionReportResponseDto.builder()
                .photoBoothId(photoBoothId)
                .message("혼잡도 제보가 반영되었습니다.")
                .submittedAt(saved.getCreatedAt())
                .build();
    }

    private double calculateDistanceInMeters(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000; // Earth radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private double calculateWeightedScore(List<CongestionReport> reports) {
        LocalDateTime now = LocalDateTime.now();
        double weightedSum = 0.0;
        double weightTotal = 0.0;

        for (CongestionReport report : reports) {
            long minutesAgo = Math.max(0, Duration.between(report.getCreatedAt(), now).toMinutes());
            // 최근 제보에 가중치 부여
            double weight = Math.exp(-(double) minutesAgo / 30.0);
            weightedSum += report.getCongestionLevel().getScore() * weight;
            weightTotal += weight;
        }
        return weightTotal == 0.0 ? 0.0 : weightedSum / weightTotal;
    }

    private CongestionReport.CongestionLevel mapScoreToLevel(double score) {
        if (score < 1.75) {
            return CongestionReport.CongestionLevel.RELAXED;
        } else if (score < 2.5) {
            return CongestionReport.CongestionLevel.NORMAL;
        } else if (score < 3.25) {
            return CongestionReport.CongestionLevel.BUSY;
        } else {
            return CongestionReport.CongestionLevel.VERY_BUSY;
        }
    }

    private int[] mapLevelToWaitRange(CongestionReport.CongestionLevel level) {
        return switch (level) {
            case RELAXED -> new int[]{0, 10};
            case NORMAL -> new int[]{10, 20};
            case BUSY -> new int[]{20, 35};
            case VERY_BUSY -> new int[]{35, 60};
            default -> new int[]{0, 0};
        };
    }

    private CongestionResponseDto.ConfidenceLevel mapConfidence(int sampleSize) {
        if (sampleSize >= 6) {
            return CongestionResponseDto.ConfidenceLevel.HIGH;
        } else if (sampleSize >= 3) {
            return CongestionResponseDto.ConfidenceLevel.MEDIUM;
        } else {
            return CongestionResponseDto.ConfidenceLevel.LOW;
        }
    }

    private String buildMessage(
            CongestionReport.CongestionLevel level,
            CongestionResponseDto.ConfidenceLevel confidence,
            int sampleSize
    ) {
        return String.format("최근 %d건 제보 기준 %s (신뢰도: %s)", sampleSize, level.name(), confidence.name());
    }
}
