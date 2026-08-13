package com.min.chalkakserver.controller;

import com.min.chalkakserver.dto.auth.*;
import com.min.chalkakserver.security.CustomUserDetails;
import com.min.chalkakserver.service.AuthService;
import com.min.chalkakserver.service.PasswordResetService;
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

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @Operation(summary = "소셜 로그인", description = "카카오/네이버/애플 소셜 로그인")
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> socialLogin(
            @Valid @RequestBody SocialLoginRequestDto request) {
        AuthResponseDto response = authService.socialLogin(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "이메일 회원가입", description = "이메일과 비밀번호로 회원가입")
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> registerWithEmail(
            @Valid @RequestBody EmailRegisterRequestDto request) {
        AuthResponseDto response = authService.registerWithEmail(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "이메일 로그인", description = "이메일과 비밀번호로 로그인")
    @PostMapping("/login/email")
    public ResponseEntity<AuthResponseDto> loginWithEmail(
            @Valid @RequestBody EmailLoginRequestDto request) {
        AuthResponseDto response = authService.loginWithEmail(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 Access Token 재발급")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDto request) {
        AuthResponseDto response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "로그아웃", description = "현재 기기에서 로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestBody RefreshTokenRequestDto request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(Map.of("message", "로그아웃 되었습니다."));
    }

    @Operation(summary = "모든 기기에서 로그아웃", description = "모든 기기에서 로그아웃 (로그인 필요)")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, String>> logoutAll(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.logoutAll(userDetails.getId());
        return ResponseEntity.ok(Map.of("message", "모든 기기에서 로그아웃 되었습니다."));
    }

    @Operation(summary = "현재 사용자 정보", description = "현재 로그인한 사용자 정보 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UserResponseDto response = authService.getCurrentUser(userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "사용자 활동 통계", description = "리뷰 수, 즐겨찾기 수 등 활동 통계")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me/stats")
    public ResponseEntity<Map<String, Object>> getUserStats(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Map<String, Object> stats = authService.getUserStats(userDetails.getId());
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "회원 탈퇴", description = "계정 삭제 (로그인 필요)")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/withdraw")
    public ResponseEntity<Map<String, String>> deleteAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.deleteAccount(userDetails.getId());
        return ResponseEntity.ok(Map.of("message", "회원 탈퇴가 완료되었습니다."));
    }

    @Operation(summary = "비밀번호 재설정 요청",
        description = "이메일 계정이 존재하면 6자리 인증코드 발송. 미가입 이메일은 404, "
            + "소셜로만 가입된 이메일은 409를 반환한다.")
    @PostMapping("/password/reset/request")
    public ResponseEntity<Map<String, String>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequestDto request) {
        passwordResetService.requestReset(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "인증코드가 발송되었습니다. 이메일을 확인해주세요."));
    }

    @Operation(summary = "비밀번호 재설정 인증코드 검증", description = "인증코드 검증 후 리셋 토큰 발급")
    @PostMapping("/password/reset/verify")
    public ResponseEntity<PasswordResetVerifyResponseDto> verifyPasswordResetCode(
            @Valid @RequestBody PasswordResetVerifyRequestDto request) {
        String resetToken = passwordResetService.verifyCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok(PasswordResetVerifyResponseDto.builder()
            .resetToken(resetToken)
            .build());
    }

    @Operation(summary = "비밀번호 재설정 확정", description = "리셋 토큰으로 새 비밀번호 설정")
    @PostMapping("/password/reset/confirm")
    public ResponseEntity<Map<String, String>> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequestDto request) {
        passwordResetService.confirmReset(request.getResetToken(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "비밀번호가 재설정되었습니다."));
    }

    @Operation(summary = "가입 수단 찾기", description = "이메일로 가입된 로그인 수단(provider) 목록 조회")
    @PostMapping("/find-provider")
    public ResponseEntity<FindProviderResponseDto> findProvider(
            @Valid @RequestBody FindProviderRequestDto request) {
        List<String> providers = passwordResetService.findProviders(request.getEmail());
        return ResponseEntity.ok(FindProviderResponseDto.builder()
            .providers(providers)
            .build());
    }

    @Operation(summary = "프로필 수정", description = "닉네임 등 프로필 정보 수정")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/profile")
    public ResponseEntity<UserResponseDto> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ProfileUpdateRequestDto request) {
        UserResponseDto response = authService.updateProfile(userDetails.getId(), request);
        return ResponseEntity.ok(response);
    }
}
