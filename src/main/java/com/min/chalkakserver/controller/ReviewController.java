package com.min.chalkakserver.controller;

import com.min.chalkakserver.dto.PagedResponseDto;
import com.min.chalkakserver.dto.review.ReviewRequestDto;
import com.min.chalkakserver.dto.review.ReviewResponseDto;
import com.min.chalkakserver.dto.review.ReviewStatsDto;
import com.min.chalkakserver.security.CustomUserDetails;
import com.min.chalkakserver.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Reviews", description = "리뷰 API")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "리뷰 작성", description = "포토부스에 리뷰 작성 (로그인 필요)")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/photo-booth/{photoBoothId}")
    public ResponseEntity<ReviewResponseDto> createReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long photoBoothId,
            @Valid @RequestBody ReviewRequestDto request) {
        ReviewResponseDto response = reviewService.createReview(
            userDetails.getId(), photoBoothId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "리뷰 수정", description = "내 리뷰 수정 (로그인 필요)")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponseDto> updateReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRequestDto request) {
        ReviewResponseDto response = reviewService.updateReview(
            userDetails.getId(), reviewId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "리뷰 삭제", description = "내 리뷰 삭제 (로그인 필요)")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Map<String, String>> deleteReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long reviewId) {
        reviewService.deleteReview(userDetails.getId(), reviewId);
        return ResponseEntity.ok(Map.of("message", "리뷰가 삭제되었습니다."));
    }

    @Operation(summary = "포토부스 리뷰 목록", description = "특정 포토부스의 리뷰 목록 조회")
    @GetMapping("/photo-booth/{photoBoothId}")
    public ResponseEntity<List<ReviewResponseDto>> getPhotoBoothReviews(
            @PathVariable Long photoBoothId) {
        List<ReviewResponseDto> response = reviewService.getPhotoBoothReviews(photoBoothId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "포토부스 리뷰 목록 (페이징)", description = "특정 포토부스의 리뷰 목록 페이징 조회")
    @GetMapping("/photo-booth/{photoBoothId}/paged")
    public ResponseEntity<PagedResponseDto<ReviewResponseDto>> getPhotoBoothReviewsPaged(
            @PathVariable Long photoBoothId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PagedResponseDto<ReviewResponseDto> response = 
            reviewService.getPhotoBoothReviewsPaged(photoBoothId, page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "포토부스 리뷰 통계", description = "특정 포토부스의 리뷰 통계 (평균 평점, 별점 분포 등)")
    @GetMapping("/photo-booth/{photoBoothId}/stats")
    public ResponseEntity<ReviewStatsDto> getReviewStats(
            @PathVariable Long photoBoothId) {
        ReviewStatsDto response = reviewService.getReviewStats(photoBoothId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "내 리뷰 목록", description = "내가 작성한 리뷰 목록 (로그인 필요)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/my")
    public ResponseEntity<List<ReviewResponseDto>> getMyReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ReviewResponseDto> response = reviewService.getMyReviews(userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "내 리뷰 목록 (페이징)", description = "내가 작성한 리뷰 목록 페이징 조회 (로그인 필요)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/my/paged")
    public ResponseEntity<PagedResponseDto<ReviewResponseDto>> getMyReviewsPaged(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PagedResponseDto<ReviewResponseDto> response = 
            reviewService.getMyReviewsPaged(userDetails.getId(), page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "특정 리뷰 조회", description = "리뷰 상세 조회")
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponseDto> getReview(
            @PathVariable Long reviewId) {
        ReviewResponseDto response = reviewService.getReview(reviewId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "내 리뷰 확인", description = "특정 포토부스에 내가 작성한 리뷰 조회 (로그인 필요)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/my/photo-booth/{photoBoothId}")
    public ResponseEntity<ReviewResponseDto> getMyReviewForPhotoBooth(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long photoBoothId) {
        ReviewResponseDto response = reviewService.getMyReviewForPhotoBooth(
            userDetails.getId(), photoBoothId);
        return ResponseEntity.ok(response);
    }
}
