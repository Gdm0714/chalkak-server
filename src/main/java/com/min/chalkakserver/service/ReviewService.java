package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.PagedResponseDto;
import com.min.chalkakserver.dto.review.ReviewRequestDto;
import com.min.chalkakserver.dto.review.ReviewResponseDto;
import com.min.chalkakserver.dto.review.ReviewStatsDto;
import com.min.chalkakserver.entity.PhotoBooth;
import com.min.chalkakserver.entity.Review;
import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.exception.AuthException;
import com.min.chalkakserver.exception.DuplicateReviewException;
import com.min.chalkakserver.exception.PhotoBoothNotFoundException;
import com.min.chalkakserver.exception.ReviewNotFoundException;
import com.min.chalkakserver.repository.PhotoBoothRepository;
import com.min.chalkakserver.repository.ReviewRepository;
import com.min.chalkakserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final PhotoBoothRepository photoBoothRepository;

    /**
     * 리뷰 작성
     */
    @Transactional
    public ReviewResponseDto createReview(Long userId, Long photoBoothId, ReviewRequestDto request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("User not found"));

        PhotoBooth photoBooth = photoBoothRepository.findById(photoBoothId)
            .orElseThrow(() -> new PhotoBoothNotFoundException(photoBoothId));

        // 이미 리뷰를 작성했는지 확인
        if (reviewRepository.existsByUserAndPhotoBooth(user, photoBooth)) {
            throw new DuplicateReviewException(photoBoothId);
        }

        Review review = Review.builder()
            .user(user)
            .photoBooth(photoBooth)
            .rating(request.getRating())
            .content(request.getContent())
            .imageUrl(request.getImageUrl())
            .build();

        Review savedReview = reviewRepository.save(review);
        log.info("Review created: userId={}, photoBoothId={}, rating={}", 
            userId, photoBoothId, request.getRating());

        return ReviewResponseDto.from(savedReview);
    }

    /**
     * 리뷰 수정
     */
    @Transactional
    public ReviewResponseDto updateReview(Long userId, Long reviewId, ReviewRequestDto request) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        // 작성자 확인
        if (!review.getUser().getId().equals(userId)) {
            throw new AuthException("리뷰를 수정할 권한이 없습니다.");
        }

        review.update(request.getRating(), request.getContent(), request.getImageUrl());
        log.info("Review updated: reviewId={}, userId={}", reviewId, userId);

        return ReviewResponseDto.from(review);
    }

    /**
     * 리뷰 삭제
     */
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        // 작성자 확인
        if (!review.getUser().getId().equals(userId)) {
            throw new AuthException("리뷰를 삭제할 권한이 없습니다.");
        }

        reviewRepository.delete(review);
        log.info("Review deleted: reviewId={}, userId={}", reviewId, userId);
    }

    /**
     * 포토부스 리뷰 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getPhotoBoothReviews(Long photoBoothId) {
        PhotoBooth photoBooth = photoBoothRepository.findById(photoBoothId)
            .orElseThrow(() -> new PhotoBoothNotFoundException(photoBoothId));

        return reviewRepository.findByPhotoBoothWithUser(photoBooth).stream()
            .map(ReviewResponseDto::from)
            .collect(Collectors.toList());
    }

    /**
     * 포토부스 리뷰 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public PagedResponseDto<ReviewResponseDto> getPhotoBoothReviewsPaged(
            Long photoBoothId, int page, int size) {
        PhotoBooth photoBooth = photoBoothRepository.findById(photoBoothId)
            .orElseThrow(() -> new PhotoBoothNotFoundException(photoBoothId));

        Pageable pageable = PageRequest.of(page, size);
        Page<Review> reviewPage = reviewRepository.findByPhotoBoothWithUserPaged(photoBooth, pageable);

        List<ReviewResponseDto> content = reviewPage.getContent().stream()
            .map(ReviewResponseDto::from)
            .collect(Collectors.toList());

        return new PagedResponseDto<>(
            content,
            reviewPage.getNumber(),
            reviewPage.getSize(),
            reviewPage.getTotalElements(),
            reviewPage.getTotalPages(),
            reviewPage.isFirst(),
            reviewPage.isLast(),
            reviewPage.hasNext(),
            reviewPage.hasPrevious()
        );
    }

    /**
     * 내가 작성한 리뷰 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getMyReviews(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("User not found"));

        return reviewRepository.findByUserWithPhotoBooth(user).stream()
            .map(ReviewResponseDto::from)
            .collect(Collectors.toList());
    }

    /**
     * 내가 작성한 리뷰 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public PagedResponseDto<ReviewResponseDto> getMyReviewsPaged(Long userId, int page, int size) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("User not found"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Review> reviewPage = reviewRepository.findByUserWithPhotoBoothPaged(user, pageable);

        List<ReviewResponseDto> content = reviewPage.getContent().stream()
            .map(ReviewResponseDto::from)
            .collect(Collectors.toList());

        return new PagedResponseDto<>(
            content,
            reviewPage.getNumber(),
            reviewPage.getSize(),
            reviewPage.getTotalElements(),
            reviewPage.getTotalPages(),
            reviewPage.isFirst(),
            reviewPage.isLast(),
            reviewPage.hasNext(),
            reviewPage.hasPrevious()
        );
    }

    /**
     * 포토부스 리뷰 통계 조회
     */
    @Transactional(readOnly = true)
    public ReviewStatsDto getReviewStats(Long photoBoothId) {
        PhotoBooth photoBooth = photoBoothRepository.findById(photoBoothId)
            .orElseThrow(() -> new PhotoBoothNotFoundException(photoBoothId));

        Double averageRating = reviewRepository.getAverageRatingByPhotoBooth(photoBooth);
        long totalCount = reviewRepository.countByPhotoBooth(photoBooth);

        // 별점 분포 조회
        Map<Integer, Long> ratingDistribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            ratingDistribution.put(i, 0L);  // 기본값 0으로 초기화
        }

        List<Object[]> distribution = reviewRepository.getRatingDistribution(photoBooth);
        for (Object[] row : distribution) {
            Integer rating = (Integer) row[0];
            Long count = (Long) row[1];
            ratingDistribution.put(rating, count);
        }

        return ReviewStatsDto.builder()
            .photoBoothId(photoBoothId)
            .averageRating(averageRating != null ? Math.round(averageRating * 10) / 10.0 : 0.0)
            .totalCount(totalCount)
            .ratingDistribution(ratingDistribution)
            .build();
    }

    /**
     * 특정 리뷰 조회
     */
    @Transactional(readOnly = true)
    public ReviewResponseDto getReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ReviewNotFoundException(reviewId));
        return ReviewResponseDto.from(review);
    }

    /**
     * 내가 이 포토부스에 작성한 리뷰 조회
     */
    @Transactional(readOnly = true)
    public ReviewResponseDto getMyReviewForPhotoBooth(Long userId, Long photoBoothId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("User not found"));

        PhotoBooth photoBooth = photoBoothRepository.findById(photoBoothId)
            .orElseThrow(() -> new PhotoBoothNotFoundException(photoBoothId));

        return reviewRepository.findByUserAndPhotoBooth(user, photoBooth)
            .map(ReviewResponseDto::from)
            .orElse(null);
    }
}
