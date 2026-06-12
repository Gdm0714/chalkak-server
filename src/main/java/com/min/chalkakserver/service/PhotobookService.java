package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.PagedResponseDto;
import com.min.chalkakserver.dto.photobook.PhotobookCollectionResponseDto;
import com.min.chalkakserver.dto.photobook.PhotobookEntryRequestDto;
import com.min.chalkakserver.dto.photobook.PhotobookEntryResponseDto;
import com.min.chalkakserver.entity.FrameCollab;
import com.min.chalkakserver.entity.PhotoBooth;
import com.min.chalkakserver.entity.PhotobookEntry;
import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.exception.AuthException;
import com.min.chalkakserver.exception.FrameCollabNotFoundException;
import com.min.chalkakserver.exception.PhotoBoothNotFoundException;
import com.min.chalkakserver.exception.PhotobookEntryNotFoundException;
import com.min.chalkakserver.repository.FrameCollabRepository;
import com.min.chalkakserver.repository.PhotoBoothRepository;
import com.min.chalkakserver.repository.PhotobookEntryRepository;
import com.min.chalkakserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotobookService {

    private final PhotobookEntryRepository photobookEntryRepository;
    private final FrameCollabRepository frameCollabRepository;
    private final PhotoBoothRepository photoBoothRepository;
    private final UserRepository userRepository;

    @Transactional
    public PhotobookEntryResponseDto createEntry(Long userId, PhotobookEntryRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("사용자를 찾을 수 없습니다."));

        FrameCollab collab = null;
        if (request.getFrameCollabId() != null) {
            collab = frameCollabRepository.findById(request.getFrameCollabId())
                    .orElseThrow(() -> new FrameCollabNotFoundException(request.getFrameCollabId()));
        }

        PhotoBooth booth = null;
        if (request.getPhotoBoothId() != null) {
            booth = photoBoothRepository.findById(request.getPhotoBoothId())
                    .orElseThrow(() -> new PhotoBoothNotFoundException(request.getPhotoBoothId()));
        }

        PhotobookEntry entry = PhotobookEntry.builder()
                .user(user)
                .frameCollab(collab)
                .photoBooth(booth)
                .imageUrl(request.getImageUrl())
                .takenAt(request.getTakenAt())
                .memo(request.getMemo())
                .build();

        PhotobookEntry saved = photobookEntryRepository.save(entry);
        log.info("Photobook entry created: userId={}, entryId={}, collabId={}",
                userId, saved.getId(), request.getFrameCollabId());
        return PhotobookEntryResponseDto.from(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponseDto<PhotobookEntryResponseDto> getMyEntries(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PhotobookEntry> entryPage = photobookEntryRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PagedResponseDto.from(entryPage.map(PhotobookEntryResponseDto::from));
    }

    @Transactional(readOnly = true)
    public PagedResponseDto<PhotobookEntryResponseDto> getMyEntriesByCollab(
            Long userId, Long frameCollabId, int page, int size) {
        if (!frameCollabRepository.existsById(frameCollabId)) {
            throw new FrameCollabNotFoundException(frameCollabId);
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<PhotobookEntry> entryPage = photobookEntryRepository
                .findByUserIdAndFrameCollabIdOrderByCreatedAtDesc(userId, frameCollabId, pageable);
        return PagedResponseDto.from(entryPage.map(PhotobookEntryResponseDto::from));
    }

    @Transactional
    public void deleteEntry(Long userId, Long entryId) {
        PhotobookEntry entry = photobookEntryRepository.findById(entryId)
                .orElseThrow(() -> new PhotobookEntryNotFoundException(entryId));

        if (!entry.getUser().getId().equals(userId)) {
            throw new AuthException("포토북 사진을 삭제할 권한이 없습니다.");
        }

        photobookEntryRepository.delete(entry);
        log.info("Photobook entry deleted: userId={}, entryId={}", userId, entryId);
    }

    @Transactional(readOnly = true)
    public PhotobookCollectionResponseDto getMyCollection(Long userId) {
        LocalDate today = LocalDate.now();

        Map<Long, Long> countByCollab = photobookEntryRepository.countByUserGroupedByCollab(userId)
                .stream()
                .collect(Collectors.toMap(
                        PhotobookEntryRepository.CollabPhotoCount::getCollabId,
                        PhotobookEntryRepository.CollabPhotoCount::getPhotoCount));

        List<PhotobookCollectionResponseDto.CollectionItemDto> items = frameCollabRepository
                .findAll(Sort.by(Sort.Direction.DESC, "startDate"))
                .stream()
                .map(collab -> PhotobookCollectionResponseDto.CollectionItemDto.from(
                        collab, today, countByCollab.getOrDefault(collab.getId(), 0L)))
                .toList();

        int collected = (int) items.stream()
                .filter(PhotobookCollectionResponseDto.CollectionItemDto::isCollected)
                .count();

        return PhotobookCollectionResponseDto.builder()
                .totalCollabs(items.size())
                .collectedCollabs(collected)
                .totalPhotos(photobookEntryRepository.countByUserId(userId))
                .items(items)
                .build();
    }
}
