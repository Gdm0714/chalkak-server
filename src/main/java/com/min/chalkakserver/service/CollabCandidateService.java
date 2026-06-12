package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.PagedResponseDto;
import com.min.chalkakserver.dto.collabcandidate.CollabCandidateApproveRequestDto;
import com.min.chalkakserver.dto.collabcandidate.CollabCandidateRequestDto;
import com.min.chalkakserver.dto.collabcandidate.CollabCandidateResponseDto;
import com.min.chalkakserver.dto.framecollab.FrameCollabDetailResponseDto;
import com.min.chalkakserver.dto.framecollab.FrameCollabRequestDto;
import com.min.chalkakserver.entity.CollabCandidate;
import com.min.chalkakserver.exception.CollabCandidateNotFoundException;
import com.min.chalkakserver.repository.CollabCandidateRepository;
import com.min.chalkakserver.util.CollabTextParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollabCandidateService {

    private final CollabCandidateRepository collabCandidateRepository;
    private final FrameCollabService frameCollabService;

    @Transactional
    public CollabCandidateResponseDto createCandidate(CollabCandidateRequestDto request) {
        CollabTextParser.ParsedCollab parsed =
                CollabTextParser.parse(request.getRawText(), LocalDate.now());

        CollabCandidate candidate = CollabCandidate.builder()
                .sourceUrl(request.getSourceUrl())
                .rawText(request.getRawText())
                .title(parsed.title())
                .brand(parsed.brand())
                .artistName(parsed.artistName())
                .startDate(parsed.startDate())
                .endDate(parsed.endDate())
                .build();

        CollabCandidate saved = collabCandidateRepository.save(candidate);
        log.info("Collab candidate created: id={}, title={}, brand={}",
                saved.getId(), saved.getTitle(), saved.getBrand());
        return CollabCandidateResponseDto.from(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponseDto<CollabCandidateResponseDto> getCandidates(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CollabCandidate> candidatePage;
        if (status == null || status.isBlank() || "all".equalsIgnoreCase(status)) {
            candidatePage = collabCandidateRepository.findAllByOrderByCreatedAtDesc(pageable);
        } else {
            CollabCandidate.CandidateStatus candidateStatus;
            try {
                candidateStatus = CollabCandidate.CandidateStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "status는 pending, approved, rejected, all 중 하나여야 합니다.");
            }
            candidatePage = collabCandidateRepository
                    .findByStatusOrderByCreatedAtDesc(candidateStatus, pageable);
        }
        return PagedResponseDto.from(candidatePage.map(CollabCandidateResponseDto::from));
    }

    @Transactional
    public FrameCollabDetailResponseDto approveCandidate(Long candidateId,
                                                         CollabCandidateApproveRequestDto request) {
        CollabCandidate candidate = collabCandidateRepository.findById(candidateId)
                .orElseThrow(() -> new CollabCandidateNotFoundException(candidateId));

        if (candidate.getStatus() != CollabCandidate.CandidateStatus.PENDING) {
            throw new IllegalArgumentException("대기 중인 후보만 승인할 수 있습니다.");
        }

        CollabCandidateApproveRequestDto overrides = request != null
                ? request
                : CollabCandidateApproveRequestDto.builder().build();

        String title = firstNonNull(overrides.getTitle(), candidate.getTitle());
        LocalDate startDate = firstNonNull(overrides.getStartDate(), candidate.getStartDate());
        LocalDate endDate = firstNonNull(overrides.getEndDate(), candidate.getEndDate());

        if (title == null || startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "승인하려면 제목/시작일/종료일이 필요합니다. 누락된 값을 함께 보내주세요.");
        }

        FrameCollabRequestDto collabRequest = FrameCollabRequestDto.builder()
                .title(title)
                .brand(firstNonNull(overrides.getBrand(), candidate.getBrand()))
                .artistName(firstNonNull(overrides.getArtistName(), candidate.getArtistName()))
                .description(overrides.getDescription())
                .imageUrl(overrides.getImageUrl())
                .startDate(startDate)
                .endDate(endDate)
                .photoBoothIds(overrides.getPhotoBoothIds())
                .build();

        FrameCollabDetailResponseDto created = frameCollabService.createCollab(collabRequest);
        candidate.approve(created.getId());
        log.info("Collab candidate approved: candidateId={}, frameCollabId={}",
                candidateId, created.getId());
        return created;
    }

    @Transactional
    public CollabCandidateResponseDto rejectCandidate(Long candidateId) {
        CollabCandidate candidate = collabCandidateRepository.findById(candidateId)
                .orElseThrow(() -> new CollabCandidateNotFoundException(candidateId));

        if (candidate.getStatus() != CollabCandidate.CandidateStatus.PENDING) {
            throw new IllegalArgumentException("대기 중인 후보만 거절할 수 있습니다.");
        }

        candidate.reject();
        log.info("Collab candidate rejected: candidateId={}", candidateId);
        return CollabCandidateResponseDto.from(candidate);
    }

    private static <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }
}
