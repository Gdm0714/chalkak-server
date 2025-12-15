package com.min.chalkakserver.controller;

import com.min.chalkakserver.dto.PagedResponseDto;
import com.min.chalkakserver.dto.favorite.FavoriteResponseDto;
import com.min.chalkakserver.security.CustomUserDetails;
import com.min.chalkakserver.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Favorites", description = "즐겨찾기 API")
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "즐겨찾기 추가", description = "포토부스를 즐겨찾기에 추가")
    @PostMapping("/{photoBoothId}")
    public ResponseEntity<FavoriteResponseDto> addFavorite(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long photoBoothId) {
        FavoriteResponseDto response = favoriteService.addFavorite(userDetails.getId(), photoBoothId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "즐겨찾기 삭제", description = "포토부스를 즐겨찾기에서 삭제")
    @DeleteMapping("/{photoBoothId}")
    public ResponseEntity<Map<String, String>> removeFavorite(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long photoBoothId) {
        favoriteService.removeFavorite(userDetails.getId(), photoBoothId);
        return ResponseEntity.ok(Map.of("message", "즐겨찾기가 삭제되었습니다."));
    }

    @Operation(summary = "내 즐겨찾기 목록", description = "내 즐겨찾기 목록 조회")
    @GetMapping
    public ResponseEntity<List<FavoriteResponseDto>> getMyFavorites(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<FavoriteResponseDto> response = favoriteService.getMyFavorites(userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "내 즐겨찾기 목록 (페이징)", description = "내 즐겨찾기 목록 페이징 조회")
    @GetMapping("/paged")
    public ResponseEntity<PagedResponseDto<FavoriteResponseDto>> getMyFavoritesPaged(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponseDto<FavoriteResponseDto> response = 
            favoriteService.getMyFavoritesPaged(userDetails.getId(), page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "즐겨찾기 여부 확인", description = "특정 포토부스가 즐겨찾기에 있는지 확인")
    @GetMapping("/check/{photoBoothId}")
    public ResponseEntity<Map<String, Boolean>> checkFavorite(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long photoBoothId) {
        boolean isFavorite = favoriteService.isFavorite(userDetails.getId(), photoBoothId);
        return ResponseEntity.ok(Map.of("isFavorite", isFavorite));
    }

    @Operation(summary = "포토부스 즐겨찾기 수", description = "특정 포토부스의 총 즐겨찾기 수 조회")
    @GetMapping("/count/{photoBoothId}")
    public ResponseEntity<Map<String, Long>> getFavoriteCount(
            @PathVariable Long photoBoothId) {
        long count = favoriteService.getFavoriteCount(photoBoothId);
        return ResponseEntity.ok(Map.of("count", count));
    }
}
