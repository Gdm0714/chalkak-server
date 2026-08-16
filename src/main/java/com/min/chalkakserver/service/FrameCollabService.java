package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.PagedResponseDto;
import com.min.chalkakserver.dto.framecollab.FrameCollabDetailResponseDto;
import com.min.chalkakserver.dto.framecollab.FrameCollabRequestDto;
import com.min.chalkakserver.dto.framecollab.FrameCollabResponseDto;
import com.min.chalkakserver.entity.FrameCollab;
import com.min.chalkakserver.entity.PhotoBooth;
import com.min.chalkakserver.exception.FrameCollabNotFoundException;
import com.min.chalkakserver.exception.PhotoBoothNotFoundException;
import com.min.chalkakserver.repository.FrameCollabRepository;
import com.min.chalkakserver.repository.PhotoBoothRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class FrameCollabService {

    private final FrameCollabRepository frameCollabRepository;
    private final PhotoBoothRepository photoBoothRepository;

    @Transactional(readOnly = true)
    public PagedResponseDto<FrameCollabResponseDto> getCollabs(String status, int page, int size) {
        LocalDate today = LocalDate.now();
        Pageable pageable = PageRequest.of(page, size);

        Page<FrameCollab> collabPage = switch (status) {
            case "upcoming" -> frameCollabRepository.findUpcoming(today, pageable);
            case "ended" -> frameCollabRepository.findEnded(today, pageable);
            default -> frameCollabRepository.findActive(today, pageable);
        };

        return PagedResponseDto.from(collabPage.map(collab -> FrameCollabResponseDto.from(collab, today)));
    }

    @Transactional(readOnly = true)
    public FrameCollabDetailResponseDto getCollab(Long id) {
        FrameCollab collab = frameCollabRepository.findByIdWithBooths(id)
                .orElseThrow(() -> new FrameCollabNotFoundException(id));
        return FrameCollabDetailResponseDto.from(collab, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<FrameCollabResponseDto> getCollabsByPhotoBooth(Long photoBoothId) {
        if (!photoBoothRepository.existsById(photoBoothId)) {
            throw new PhotoBoothNotFoundException(photoBoothId);
        }
        LocalDate today = LocalDate.now();
        return frameCollabRepository.findCurrentByPhotoBoothId(photoBoothId, today).stream()
                .map(collab -> FrameCollabResponseDto.from(collab, today))
                .toList();
    }

    @Transactional
    public FrameCollabDetailResponseDto createCollab(FrameCollabRequestDto request) {
        FrameCollab collab = FrameCollab.builder()
                .title(request.getTitle())
                .brand(request.getBrand())
                .artistName(request.getArtistName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        if (request.getPhotoBoothIds() != null && !request.getPhotoBoothIds().isEmpty()) {
            collab.updatePhotoBooths(resolveBooths(request.getPhotoBoothIds()));
        }

        FrameCollab saved = frameCollabRepository.save(collab);
        log.info("FrameCollab created: id={}, title={}", saved.getId(), saved.getTitle());
        return FrameCollabDetailResponseDto.from(saved, LocalDate.now());
    }

    @Transactional
    public FrameCollabDetailResponseDto updateCollab(Long id, FrameCollabRequestDto request) {
        FrameCollab collab = frameCollabRepository.findByIdWithBooths(id)
                .orElseThrow(() -> new FrameCollabNotFoundException(id));

        collab.update(request.getTitle(), request.getBrand(), request.getArtistName(),
                request.getDescription(), request.getImageUrl(),
                request.getStartDate(), request.getEndDate());

        if (request.getPhotoBoothIds() != null) {
            collab.updatePhotoBooths(resolveBooths(request.getPhotoBoothIds()));
        }

        log.info("FrameCollab updated: id={}", id);
        return FrameCollabDetailResponseDto.from(collab, LocalDate.now());
    }

    @Transactional
    public void deleteCollab(Long id) {
        FrameCollab collab = frameCollabRepository.findById(id)
                .orElseThrow(() -> new FrameCollabNotFoundException(id));
        frameCollabRepository.delete(collab);
        log.info("FrameCollab deleted: id={}", id);
    }

    @Transactional
    public FrameCollabDetailResponseDto setPhotoBooths(Long id, List<Long> photoBoothIds) {
        FrameCollab collab = frameCollabRepository.findByIdWithBooths(id)
                .orElseThrow(() -> new FrameCollabNotFoundException(id));
        collab.updatePhotoBooths(resolveBooths(photoBoothIds));
        log.info("FrameCollab booths updated: id={}, boothCount={}", id, photoBoothIds.size());
        return FrameCollabDetailResponseDto.from(collab, LocalDate.now());
    }

    private Set<PhotoBooth> resolveBooths(List<Long> photoBoothIds) {
        List<PhotoBooth> booths = photoBoothRepository.findAllById(photoBoothIds);
        if (booths.size() != Set.copyOf(photoBoothIds).size()) {
            Set<Long> foundIds = new HashSet<>();
            for (PhotoBooth booth : booths) {
                foundIds.add(booth.getId());
            }
            List<Long> missing = photoBoothIds.stream()
                    .filter(boothId -> !foundIds.contains(boothId))
                    .toList();
            throw new PhotoBoothNotFoundException("존재하지 않는 사진관이 포함되어 있습니다. ID: " + missing);
        }
        return new HashSet<>(booths);
    }
}
