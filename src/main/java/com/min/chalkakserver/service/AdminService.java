package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.PagedResponseDto;
import com.min.chalkakserver.dto.PhotoBoothRequestDto;
import com.min.chalkakserver.dto.PhotoBoothResponseDto;
import com.min.chalkakserver.dto.admin.AdminStatsDto;
import com.min.chalkakserver.dto.admin.UserListResponseDto;
import com.min.chalkakserver.entity.PhotoBooth;
import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.exception.AuthException;
import com.min.chalkakserver.exception.PhotoBoothNotFoundException;
import com.min.chalkakserver.exception.ReviewNotFoundException;
import com.min.chalkakserver.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final PhotoBoothRepository photoBoothRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final FavoriteRepository favoriteRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 관리자 대시보드 통계
     */
    @Transactional(readOnly = true)
    public AdminStatsDto getStats() {
        long totalPhotoBooths = photoBoothRepository.count();
        long totalUsers = userRepository.count();
        long totalReviews = reviewRepository.count();
        long totalFavorites = favoriteRepository.count();

        // 오늘 가입한 사용자 수
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long newUsersToday = userRepository.countByCreatedAtAfter(startOfDay);
        long newReviewsToday = reviewRepository.countByCreatedAtAfter(startOfDay);

        return AdminStatsDto.builder()
            .totalPhotoBooths(totalPhotoBooths)
            .totalUsers(totalUsers)
            .totalReviews(totalReviews)
            .totalFavorites(totalFavorites)
            .newUsersToday(newUsersToday)
            .newReviewsToday(newReviewsToday)
            .generatedAt(LocalDateTime.now())
            .build();
    }

    /**
     * 포토부스 생성
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "photoBooths", allEntries = true),
        @CacheEvict(value = "nearbyPhotoBooths", allEntries = true),
        @CacheEvict(value = "searchResults", allEntries = true),
        @CacheEvict(value = "brandPhotoBooths", allEntries = true)
    })
    public PhotoBoothResponseDto createPhotoBooth(PhotoBoothRequestDto request) {
        PhotoBooth photoBooth = PhotoBooth.builder()
            .name(request.getName())
            .brand(request.getBrand())
            .series(request.getSeries())
            .address(request.getAddress())
            .roadAddress(request.getRoadAddress())
            .latitude(request.getLatitude())
            .longitude(request.getLongitude())
            .operatingHours(request.getOperatingHours())
            .phoneNumber(request.getPhoneNumber())
            .description(request.getDescription())
            .priceInfo(request.getPriceInfo())
            .build();

        PhotoBooth savedPhotoBooth = photoBoothRepository.save(photoBooth);
        log.info("PhotoBooth created by admin: id={}, name={}", savedPhotoBooth.getId(), savedPhotoBooth.getName());

        return PhotoBoothResponseDto.from(savedPhotoBooth);
    }

    /**
     * 포토부스 수정
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "photoBooths", allEntries = true),
        @CacheEvict(value = "photoBooth", key = "#id"),
        @CacheEvict(value = "nearbyPhotoBooths", allEntries = true),
        @CacheEvict(value = "searchResults", allEntries = true),
        @CacheEvict(value = "brandPhotoBooths", allEntries = true)
    })
    public PhotoBoothResponseDto updatePhotoBooth(Long id, PhotoBoothRequestDto request) {
        PhotoBooth photoBooth = photoBoothRepository.findById(id)
            .orElseThrow(() -> new PhotoBoothNotFoundException(id));

        photoBooth.update(
            request.getName(),
            request.getBrand(),
            request.getSeries(),
            request.getAddress(),
            request.getRoadAddress(),
            request.getLatitude(),
            request.getLongitude(),
            request.getOperatingHours(),
            request.getPhoneNumber(),
            request.getDescription(),
            request.getPriceInfo()
        );

        log.info("PhotoBooth updated by admin: id={}", id);
        return PhotoBoothResponseDto.from(photoBooth);
    }

    /**
     * 포토부스 삭제
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "photoBooths", allEntries = true),
        @CacheEvict(value = "photoBooth", key = "#id"),
        @CacheEvict(value = "nearbyPhotoBooths", allEntries = true),
        @CacheEvict(value = "searchResults", allEntries = true),
        @CacheEvict(value = "brandPhotoBooths", allEntries = true)
    })
    public void deletePhotoBooth(Long id) {
        PhotoBooth photoBooth = photoBoothRepository.findById(id)
            .orElseThrow(() -> new PhotoBoothNotFoundException(id));

        photoBoothRepository.delete(photoBooth);
        log.info("PhotoBooth deleted by admin: id={}", id);
    }

    /**
     * 사용자 목록 조회
     */
    @Transactional(readOnly = true)
    public PagedResponseDto<UserListResponseDto> getUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> userPage = userRepository.findAll(pageable);

        List<UserListResponseDto> content = userPage.getContent().stream()
            .map(user -> {
                long reviewCount = reviewRepository.countByUser(user);
                long favoriteCount = favoriteRepository.countByUser(user);
                return UserListResponseDto.from(user, reviewCount, favoriteCount);
            })
            .collect(Collectors.toList());

        return new PagedResponseDto<>(
            content,
            userPage.getNumber(),
            userPage.getSize(),
            userPage.getTotalElements(),
            userPage.getTotalPages(),
            userPage.isFirst(),
            userPage.isLast(),
            userPage.hasNext(),
            userPage.hasPrevious()
        );
    }

    /**
     * 사용자 상세 조회
     */
    @Transactional(readOnly = true)
    public UserListResponseDto getUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("User not found"));

        long reviewCount = reviewRepository.countByUser(user);
        long favoriteCount = favoriteRepository.countByUser(user);

        return UserListResponseDto.from(user, reviewCount, favoriteCount);
    }

    /**
     * 사용자 권한 변경
     */
    @Transactional
    public UserListResponseDto updateUserRole(Long userId, String role) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("User not found"));

        User.Role newRole;
        try {
            newRole = User.Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }

        user.updateRole(newRole);
        log.info("User role updated by admin: userId={}, newRole={}", userId, newRole);

        long reviewCount = reviewRepository.countByUser(user);
        long favoriteCount = favoriteRepository.countByUser(user);

        return UserListResponseDto.from(user, reviewCount, favoriteCount);
    }

    /**
     * 사용자 삭제
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("User not found"));

        // 관련 데이터 삭제
        refreshTokenRepository.deleteAllByUser(user);
        reviewRepository.deleteAllByUser(user);
        favoriteRepository.deleteAllByUser(user);
        userRepository.delete(user);

        log.info("User deleted by admin: userId={}", userId);
    }

    /**
     * 리뷰 삭제 (관리자)
     */
    @Transactional
    public void deleteReview(Long reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new ReviewNotFoundException(reviewId);
        }

        reviewRepository.deleteById(reviewId);
        log.info("Review deleted by admin: reviewId={}", reviewId);
    }
}
