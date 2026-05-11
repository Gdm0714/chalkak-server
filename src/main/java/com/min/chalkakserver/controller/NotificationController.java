package com.min.chalkakserver.controller;

import com.min.chalkakserver.dto.PagedResponseDto;
import com.min.chalkakserver.dto.notification.DeviceTokenRequestDto;
import com.min.chalkakserver.dto.notification.NotificationResponseDto;
import com.min.chalkakserver.security.CustomUserDetails;
import com.min.chalkakserver.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Notifications", description = "푸시 알림 API")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "디바이스 토큰 등록", description = "FCM 디바이스 토큰 등록")
    @PostMapping("/device-token")
    public ResponseEntity<Map<String, String>> registerDeviceToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody DeviceTokenRequestDto request) {
        notificationService.registerDeviceToken(
                userDetails.getId(), request.getFcmToken(), request.getPlatform());
        return ResponseEntity.ok(Map.of("message", "디바이스 토큰이 등록되었습니다."));
    }

    @Operation(summary = "알림 목록 조회", description = "내 알림 목록 페이징 조회")
    @GetMapping
    public ResponseEntity<PagedResponseDto<NotificationResponseDto>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponseDto<NotificationResponseDto> response =
                notificationService.getNotifications(userDetails.getId(), page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음으로 처리")
    @PatchMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        notificationService.markAsRead(id, userDetails.getId());
        return ResponseEntity.ok(Map.of("message", "알림이 읽음 처리되었습니다."));
    }

    @Operation(summary = "전체 알림 읽음 처리", description = "모든 알림을 읽음으로 처리")
    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.markAllAsRead(userDetails.getId());
        return ResponseEntity.ok(Map.of("message", "모든 알림이 읽음 처리되었습니다."));
    }

    @Operation(summary = "읽지 않은 알림 수", description = "읽지 않은 알림 개수 조회")
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        long count = notificationService.getUnreadCount(userDetails.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }
}
