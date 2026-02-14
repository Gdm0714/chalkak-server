package com.min.chalkakserver.controller;

import com.min.chalkakserver.dto.auth.*;
import com.min.chalkakserver.security.CustomUserDetails;
import com.min.chalkakserver.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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

    @Operation(summary = "회원 탈퇴", description = "계정 삭제 (로그인 필요)")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/withdraw")
    public ResponseEntity<Map<String, String>> deleteAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.deleteAccount(userDetails.getId());
        return ResponseEntity.ok(Map.of("message", "회원 탈퇴가 완료되었습니다."));
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
