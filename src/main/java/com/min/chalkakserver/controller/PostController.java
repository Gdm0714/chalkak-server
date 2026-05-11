package com.min.chalkakserver.controller;

import com.min.chalkakserver.dto.PagedResponseDto;
import com.min.chalkakserver.dto.post.PostRequestDto;
import com.min.chalkakserver.dto.post.PostResponseDto;
import com.min.chalkakserver.security.CustomUserDetails;
import com.min.chalkakserver.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Posts", description = "사진 공유 피드 API")
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(summary = "피드 조회", description = "전체 피드 조회 (공개)")
    @GetMapping
    public ResponseEntity<PagedResponseDto<PostResponseDto>> getFeed(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(postService.getFeed(userId, page, size));
    }

    @Operation(summary = "인기 게시물 조회", description = "최근 7일간 좋아요 많은 순 (공개)")
    @GetMapping("/popular")
    public ResponseEntity<PagedResponseDto<PostResponseDto>> getPopularPosts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(postService.getPopularPosts(userId, page, size));
    }

    @Operation(summary = "주변 게시물 조회", description = "내 위치 기준 반경 내 게시물 (공개)")
    @GetMapping("/nearby")
    public ResponseEntity<PagedResponseDto<PostResponseDto>> getNearbyPosts(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "5.0") double radius,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(
            postService.getNearbyPosts(latitude, longitude, radius, userId, page, size));
    }

    @Operation(summary = "포토부스 피드 조회", description = "특정 포토부스의 게시물 조회 (공개)")
    @GetMapping("/photo-booth/{photoBoothId}")
    public ResponseEntity<PagedResponseDto<PostResponseDto>> getPhotoBoothPosts(
            @PathVariable Long photoBoothId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(postService.getPhotoBoothPosts(photoBoothId, userId, page, size));
    }

    @Operation(summary = "내 게시물 조회", description = "내가 올린 게시물 조회 (로그인 필요)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/my")
    public ResponseEntity<PagedResponseDto<PostResponseDto>> getMyPosts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(postService.getMyPosts(userDetails.getId(), page, size));
    }

    @Operation(summary = "게시물 작성", description = "사진 게시물 업로드 (로그인 필요)")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<PostResponseDto> createPost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PostRequestDto request) {
        return ResponseEntity.ok(postService.createPost(userDetails.getId(), request));
    }

    @Operation(summary = "게시물 삭제", description = "내 게시물 삭제 (로그인 필요, 본인만 가능)")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{postId}")
    public ResponseEntity<Map<String, String>> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        postService.deletePost(postId, userDetails.getId());
        return ResponseEntity.ok(Map.of("message", "게시물이 삭제되었습니다."));
    }

    @Operation(summary = "좋아요 토글", description = "게시물 좋아요/취소 (로그인 필요)")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{postId}/like")
    public ResponseEntity<Map<String, Boolean>> toggleLike(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean isLiked = postService.toggleLike(postId, userDetails.getId());
        return ResponseEntity.ok(Map.of("isLiked", isLiked));
    }

    @Operation(summary = "좋아요 여부 확인", description = "게시물 좋아요 여부 확인 (로그인 필요)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{postId}/like/check")
    public ResponseEntity<Map<String, Boolean>> checkLike(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean isLiked = postService.checkLiked(postId, userDetails.getId());
        return ResponseEntity.ok(Map.of("isLiked", isLiked));
    }
}
