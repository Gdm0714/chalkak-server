package com.min.chalkakserver.controller;

import com.min.chalkakserver.dto.PagedResponseDto;
import com.min.chalkakserver.dto.PhotoBoothRequestDto;
import com.min.chalkakserver.dto.PhotoBoothResponseDto;
import com.min.chalkakserver.dto.admin.AdminStatsDto;
import com.min.chalkakserver.dto.admin.UserListResponseDto;
import com.min.chalkakserver.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Admin", description = "관리자 API")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // ==================== 대시보드 ====================

    @Operation(summary = "관리자 대시보드 통계", description = "전체 통계 정보 조회")
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDto> getStats() {
        AdminStatsDto response = adminService.getStats();
        return ResponseEntity.ok(response);
    }

    // ==================== 포토부스 관리 ====================

    @Operation(summary = "포토부스 생성", description = "새 포토부스 등록")
    @PostMapping("/photo-booths")
    public ResponseEntity<PhotoBoothResponseDto> createPhotoBooth(
            @Valid @RequestBody PhotoBoothRequestDto request) {
        PhotoBoothResponseDto response = adminService.createPhotoBooth(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "포토부스 수정", description = "포토부스 정보 수정")
    @PutMapping("/photo-booths/{id}")
    public ResponseEntity<PhotoBoothResponseDto> updatePhotoBooth(
            @PathVariable Long id,
            @Valid @RequestBody PhotoBoothRequestDto request) {
        PhotoBoothResponseDto response = adminService.updatePhotoBooth(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "포토부스 삭제", description = "포토부스 삭제")
    @DeleteMapping("/photo-booths/{id}")
    public ResponseEntity<Map<String, String>> deletePhotoBooth(@PathVariable Long id) {
        adminService.deletePhotoBooth(id);
        return ResponseEntity.ok(Map.of("message", "포토부스가 삭제되었습니다."));
    }

    // ==================== 사용자 관리 ====================

    @Operation(summary = "사용자 목록", description = "전체 사용자 목록 조회")
    @GetMapping("/users")
    public ResponseEntity<PagedResponseDto<UserListResponseDto>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponseDto<UserListResponseDto> response = adminService.getUsers(page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "사용자 상세", description = "사용자 상세 정보 조회")
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserListResponseDto> getUser(@PathVariable Long userId) {
        UserListResponseDto response = adminService.getUser(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "사용자 권한 변경", description = "사용자 역할 변경 (USER/ADMIN)")
    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<UserListResponseDto> updateUserRole(
            @PathVariable Long userId,
            @RequestParam String role) {
        UserListResponseDto response = adminService.updateUserRole(userId, role);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "사용자 강제 탈퇴", description = "사용자 계정 삭제")
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.ok(Map.of("message", "사용자가 삭제되었습니다."));
    }

    // ==================== 리뷰 관리 ====================

    @Operation(summary = "리뷰 삭제 (관리자)", description = "부적절한 리뷰 삭제")
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Map<String, String>> deleteReview(@PathVariable Long reviewId) {
        adminService.deleteReview(reviewId);
        return ResponseEntity.ok(Map.of("message", "리뷰가 삭제되었습니다."));
    }
}
