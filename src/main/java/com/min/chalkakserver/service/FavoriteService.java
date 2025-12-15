package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.PagedResponseDto;
import com.min.chalkakserver.dto.favorite.FavoriteResponseDto;
import com.min.chalkakserver.entity.Favorite;
import com.min.chalkakserver.entity.PhotoBooth;
import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.exception.AuthException;
import com.min.chalkakserver.exception.PhotoBoothNotFoundException;
import com.min.chalkakserver.repository.FavoriteRepository;
import com.min.chalkakserver.repository.PhotoBoothRepository;
import com.min.chalkakserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final PhotoBoothRepository photoBoothRepository;

    /**
     * 즐겨찾기 추가
     */
    @Transactional
    public FavoriteResponseDto addFavorite(Long userId, Long photoBoothId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("User not found"));

        PhotoBooth photoBooth = photoBoothRepository.findById(photoBoothId)
            .orElseThrow(() -> new PhotoBoothNotFoundException(photoBoothId));

        // 이미 즐겨찾기에 있는지 확인
        if (favoriteRepository.existsByUserAndPhotoBooth(user, photoBooth)) {
            log.info("PhotoBooth already in favorites: userId={}, photoBoothId={}", userId, photoBoothId);
            return favoriteRepository.findByUserAndPhotoBooth(user, photoBooth)
                .map(FavoriteResponseDto::from)
                .orElseThrow();
        }

        Favorite favorite = Favorite.builder()
            .user(user)
            .photoBooth(photoBooth)
            .build();

        Favorite savedFavorite = favoriteRepository.save(favorite);
        log.info("Favorite added: userId={}, photoBoothId={}", userId, photoBoothId);

        return FavoriteResponseDto.from(savedFavorite);
    }

    /**
     * 즐겨찾기 삭제
     */
    @Transactional
    public void removeFavorite(Long userId, Long photoBoothId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("User not found"));

        PhotoBooth photoBooth = photoBoothRepository.findById(photoBoothId)
            .orElseThrow(() -> new PhotoBoothNotFoundException(photoBoothId));

        favoriteRepository.deleteByUserAndPhotoBooth(user, photoBooth);
        log.info("Favorite removed: userId={}, photoBoothId={}", userId, photoBoothId);
    }

    /**
     * 내 즐겨찾기 목록 조회
     */
    @Transactional(readOnly = true)
    public List<FavoriteResponseDto> getMyFavorites(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("User not found"));

        return favoriteRepository.findByUserWithPhotoBooth(user).stream()
            .map(FavoriteResponseDto::from)
            .collect(Collectors.toList());
    }

    /**
     * 내 즐겨찾기 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public PagedResponseDto<FavoriteResponseDto> getMyFavoritesPaged(Long userId, int page, int size) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("User not found"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Favorite> favoritePage = favoriteRepository.findByUserWithPhotoBoothPaged(user, pageable);

        List<FavoriteResponseDto> content = favoritePage.getContent().stream()
            .map(FavoriteResponseDto::from)
            .collect(Collectors.toList());

        return new PagedResponseDto<>(
            content,
            favoritePage.getNumber(),
            favoritePage.getSize(),
            favoritePage.getTotalElements(),
            favoritePage.getTotalPages(),
            favoritePage.isFirst(),
            favoritePage.isLast(),
            favoritePage.hasNext(),
            favoritePage.hasPrevious()
        );
    }

    /**
     * 특정 포토부스가 즐겨찾기에 있는지 확인
     */
    @Transactional(readOnly = true)
    public boolean isFavorite(Long userId, Long photoBoothId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("User not found"));

        PhotoBooth photoBooth = photoBoothRepository.findById(photoBoothId)
            .orElseThrow(() -> new PhotoBoothNotFoundException(photoBoothId));

        return favoriteRepository.existsByUserAndPhotoBooth(user, photoBooth);
    }

    /**
     * 포토부스의 즐겨찾기 수 조회
     */
    @Transactional(readOnly = true)
    public long getFavoriteCount(Long photoBoothId) {
        PhotoBooth photoBooth = photoBoothRepository.findById(photoBoothId)
            .orElseThrow(() -> new PhotoBoothNotFoundException(photoBoothId));

        return favoriteRepository.countByPhotoBooth(photoBooth);
    }
}
