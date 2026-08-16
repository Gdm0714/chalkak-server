package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.frameavailability.FrameAvailabilityReportRequestDto;
import com.min.chalkakserver.dto.frameavailability.FrameAvailabilityReportResponseDto;
import com.min.chalkakserver.dto.frameavailability.FrameAvailabilityResponseDto;
import com.min.chalkakserver.entity.FrameAvailabilityReport;
import com.min.chalkakserver.entity.FrameCollab;
import com.min.chalkakserver.entity.PhotoBooth;
import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.exception.DuplicateAvailabilityReportException;
import com.min.chalkakserver.exception.FrameCollabNotFoundException;
import com.min.chalkakserver.exception.PhotoBoothNotFoundException;
import com.min.chalkakserver.repository.FrameAvailabilityReportRepository;
import com.min.chalkakserver.repository.FrameCollabRepository;
import com.min.chalkakserver.repository.PhotoBoothRepository;
import com.min.chalkakserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FrameAvailabilityService {

    private static final int AGGREGATION_WINDOW_HOURS = 24;
    private static final int REPORT_COOLDOWN_MINUTES = 60;
    // 재고는 혼잡도보다 천천히 변하므로 감쇠 시간 상수를 6시간으로 둔다
    private static final double DECAY_TIME_CONSTANT_MINUTES = 360.0;
    private static final double AVAILABLE_THRESHOLD = 0.5;

    private final FrameAvailabilityReportRepository availabilityReportRepository;
    private final FrameCollabRepository frameCollabRepository;
    private final PhotoBoothRepository photoBoothRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public FrameAvailabilityResponseDto getAvailability(Long frameCollabId, Long photoBoothId) {
        validateCollabBooth(frameCollabId, photoBoothId);

        LocalDateTime cutoff = LocalDateTime.now().minusHours(AGGREGATION_WINDOW_HOURS);
        List<FrameAvailabilityReport> reports = availabilityReportRepository
                .findByFrameCollabIdAndPhotoBoothIdAndCreatedAtAfterOrderByCreatedAtDesc(
                        frameCollabId, photoBoothId, cutoff);

        return aggregate(frameCollabId, photoBoothId, reports);
    }

    @Transactional(readOnly = true)
    public List<FrameAvailabilityResponseDto> getCollabAvailability(Long frameCollabId) {
        FrameCollab collab = frameCollabRepository.findByIdWithBooths(frameCollabId)
                .orElseThrow(() -> new FrameCollabNotFoundException(frameCollabId));

        LocalDateTime cutoff = LocalDateTime.now().minusHours(AGGREGATION_WINDOW_HOURS);
        Map<Long, List<FrameAvailabilityReport>> reportsByBooth = availabilityReportRepository
                .findByFrameCollabIdAndCreatedAtAfterOrderByCreatedAtDesc(frameCollabId, cutoff)
                .stream()
                .collect(Collectors.groupingBy(report -> report.getPhotoBooth().getId()));

        return collab.getPhotoBooths().stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .map(booth -> aggregate(frameCollabId, booth.getId(),
                        reportsByBooth.getOrDefault(booth.getId(), List.of())))
                .toList();
    }

    @Transactional
    public FrameAvailabilityReportResponseDto submitReport(Long userId, Long frameCollabId, Long photoBoothId,
                                                           FrameAvailabilityReportRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        FrameCollab collab = frameCollabRepository.findById(frameCollabId)
                .orElseThrow(() -> new FrameCollabNotFoundException(frameCollabId));
        PhotoBooth photoBooth = photoBoothRepository.findById(photoBoothId)
                .orElseThrow(() -> new PhotoBoothNotFoundException(photoBoothId));

        validateCollabBooth(frameCollabId, photoBoothId);

        if (collab.getStatus(LocalDate.now()) != FrameCollab.CollabStatus.ACTIVE) {
            throw new IllegalArgumentException("진행 중인 콜라보만 재고 제보가 가능합니다.");
        }

        LocalDateTime cooldownCutoff = LocalDateTime.now().minusMinutes(REPORT_COOLDOWN_MINUTES);
        boolean alreadyReported = availabilityReportRepository
                .existsByUserAndFrameCollabAndPhotoBoothAndCreatedAtAfter(user, collab, photoBooth, cooldownCutoff);
        if (alreadyReported) {
            throw new DuplicateAvailabilityReportException(frameCollabId, photoBoothId);
        }

        FrameAvailabilityReport report = FrameAvailabilityReport.builder()
                .user(user)
                .frameCollab(collab)
                .photoBooth(photoBooth)
                .availabilityStatus(request.getAvailabilityStatus())
                .build();

        FrameAvailabilityReport saved = availabilityReportRepository.save(report);
        log.info("Frame availability report submitted: userId={}, collabId={}, boothId={}, status={}",
                userId, frameCollabId, photoBoothId, request.getAvailabilityStatus());

        return FrameAvailabilityReportResponseDto.builder()
                .frameCollabId(frameCollabId)
                .photoBoothId(photoBoothId)
                .message("재고 제보가 반영되었습니다.")
                .submittedAt(saved.getCreatedAt())
                .build();
    }

    private void validateCollabBooth(Long frameCollabId, Long photoBoothId) {
        if (!frameCollabRepository.existsById(frameCollabId)) {
            throw new FrameCollabNotFoundException(frameCollabId);
        }
        if (!frameCollabRepository.existsByIdAndPhotoBoothsId(frameCollabId, photoBoothId)) {
            throw new IllegalArgumentException("해당 콜라보의 입고 매장이 아닙니다.");
        }
    }

    private FrameAvailabilityResponseDto aggregate(Long frameCollabId, Long photoBoothId,
                                                   List<FrameAvailabilityReport> reports) {
        if (reports.isEmpty()) {
            return FrameAvailabilityResponseDto.builder()
                    .frameCollabId(frameCollabId)
                    .photoBoothId(photoBoothId)
                    .status(FrameAvailabilityResponseDto.ResultStatus.UNKNOWN)
                    .confidenceLevel(FrameAvailabilityResponseDto.ConfidenceLevel.LOW)
                    .sampleSize(0)
                    .message("아직 재고 제보가 없어요.")
                    .build();
        }

        double weightedScore = calculateWeightedScore(reports);
        FrameAvailabilityResponseDto.ResultStatus status = weightedScore >= AVAILABLE_THRESHOLD
                ? FrameAvailabilityResponseDto.ResultStatus.AVAILABLE
                : FrameAvailabilityResponseDto.ResultStatus.SOLD_OUT;
        FrameAvailabilityResponseDto.ConfidenceLevel confidence = mapConfidence(reports.size());

        return FrameAvailabilityResponseDto.builder()
                .frameCollabId(frameCollabId)
                .photoBoothId(photoBoothId)
                .status(status)
                .confidenceLevel(confidence)
                .sampleSize(reports.size())
                .lastUpdatedAt(reports.get(0).getCreatedAt())
                .message(String.format("최근 %d건 제보 기준 %s (신뢰도: %s)",
                        reports.size(), status.name(), confidence.name()))
                .build();
    }

    private double calculateWeightedScore(List<FrameAvailabilityReport> reports) {
        LocalDateTime now = LocalDateTime.now();
        double weightedSum = 0.0;
        double weightTotal = 0.0;

        for (FrameAvailabilityReport report : reports) {
            long minutesAgo = Math.max(0, Duration.between(report.getCreatedAt(), now).toMinutes());
            double weight = Math.exp(-minutesAgo / DECAY_TIME_CONSTANT_MINUTES);
            weightedSum += report.getAvailabilityStatus().getScore() * weight;
            weightTotal += weight;
        }
        return weightTotal == 0.0 ? 0.0 : weightedSum / weightTotal;
    }

    private FrameAvailabilityResponseDto.ConfidenceLevel mapConfidence(int sampleSize) {
        if (sampleSize >= 6) {
            return FrameAvailabilityResponseDto.ConfidenceLevel.HIGH;
        } else if (sampleSize >= 3) {
            return FrameAvailabilityResponseDto.ConfidenceLevel.MEDIUM;
        } else {
            return FrameAvailabilityResponseDto.ConfidenceLevel.LOW;
        }
    }
}
