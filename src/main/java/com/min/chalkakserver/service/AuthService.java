package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.auth.*;
import com.min.chalkakserver.entity.RefreshToken;
import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.entity.User.AuthProvider;
import com.min.chalkakserver.exception.AuthException;
import com.min.chalkakserver.repository.RefreshTokenRepository;
import com.min.chalkakserver.repository.UserRepository;
import com.min.chalkakserver.security.CustomUserDetails;
import com.min.chalkakserver.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final SocialAuthService socialAuthService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 소셜 로그인 처리
     */
    @Transactional
    public AuthResponseDto socialLogin(SocialLoginRequestDto request) {
        // 소셜 서비스에서 사용자 정보 가져오기
        SocialUserInfo socialUserInfo = socialAuthService.getSocialUserInfo(
            request.getProvider(), request.getAccessToken());

        AuthProvider provider = AuthProvider.valueOf(request.getProvider().toUpperCase());

        // 기존 사용자 조회 또는 신규 생성
        User user = userRepository.findByProviderAndProviderId(provider, socialUserInfo.getId())
            .map(existingUser -> {
                existingUser.updateProfile(socialUserInfo.getNickname(), socialUserInfo.getProfileImageUrl());
                existingUser.updateLastLogin();
                return existingUser;
            })
            .orElseGet(() -> createNewUser(socialUserInfo, provider));

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);

        // Refresh Token 저장
        saveRefreshToken(user, refreshToken, request.getDeviceInfo());

        log.info("User logged in: userId={}, provider={}", user.getId(), provider);

        return AuthResponseDto.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtTokenProvider.getAccessTokenValidity() / 1000)
            .user(UserResponseDto.from(user))
            .build();
    }

    /**
     * 토큰 갱신 (Token Rotation 적용)
     * - 매 갱신 시 새로운 refresh token 발급
     * - 이미 사용된 토큰 재사용 시 해당 토큰 패밀리의 모든 토큰 무효화 (보안 조치)
     */
    @Transactional
    public AuthResponseDto refreshToken(RefreshTokenRequestDto request) {
        String requestRefreshToken = request.getRefreshToken();

        // Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(requestRefreshToken)) {
            throw new AuthException("Invalid refresh token");
        }

        // DB에서 Refresh Token 조회
        RefreshToken storedToken = refreshTokenRepository.findByToken(requestRefreshToken)
            .orElseThrow(() -> new AuthException("Refresh token not found"));

        // 토큰 재사용 감지 (이미 사용된 토큰이 다시 사용됨)
        // 이는 토큰 탈취 가능성을 의미하므로 해당 토큰 패밀리의 모든 토큰 무효화
        if (storedToken.isUsed()) {
            log.warn("Refresh token reuse detected! tokenFamily={}, userId={}", 
                storedToken.getTokenFamily(), storedToken.getUser().getId());
            
            // 보안 조치: 해당 토큰 패밀리의 모든 토큰 삭제
            if (storedToken.getTokenFamily() != null) {
                refreshTokenRepository.deleteAllByTokenFamily(storedToken.getTokenFamily());
            }
            
            throw new AuthException("Refresh token has been revoked due to suspicious activity");
        }

        // 만료 확인
        if (storedToken.isExpired()) {
            refreshTokenRepository.delete(storedToken);
            throw new AuthException("Refresh token expired");
        }

        User user = storedToken.getUser();

        // 기존 토큰을 사용됨으로 표시 (Token Rotation)
        storedToken.markAsUsed();

        // 새 토큰 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(user);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user);

        // 새 Refresh Token 저장 (같은 토큰 패밀리 유지)
        LocalDateTime newExpiresAt = LocalDateTime.ofInstant(
            jwtTokenProvider.getRefreshTokenExpiryDate().toInstant(),
            ZoneId.systemDefault()
        );
        
        RefreshToken newToken = RefreshToken.builder()
            .user(user)
            .token(newRefreshToken)
            .deviceInfo(storedToken.getDeviceInfo())
            .expiresAt(newExpiresAt)
            .tokenFamily(storedToken.getTokenFamily())  // 같은 토큰 패밀리 유지
            .build();
        
        refreshTokenRepository.save(newToken);

        log.info("Token refreshed for user: userId={}", user.getId());

        return AuthResponseDto.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtTokenProvider.getAccessTokenValidity() / 1000)
            .user(UserResponseDto.from(user))
            .build();
    }

    /**
     * 로그아웃
     */
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.deleteByToken(refreshToken);
        log.info("User logged out, refresh token deleted");
    }

    /**
     * 모든 기기에서 로그아웃
     */
    @Transactional
    public void logoutAll(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("User not found"));
        refreshTokenRepository.deleteAllByUser(user);
        log.info("User logged out from all devices: userId={}", userId);
    }

    /**
     * 회원 탈퇴
     */
    @Transactional
    public void deleteAccount(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("User not found"));
        
        // Refresh Token 모두 삭제
        refreshTokenRepository.deleteAllByUser(user);
        
        // 사용자 삭제
        userRepository.delete(user);
        
        log.info("User account deleted: userId={}", userId);
    }

    /**
     * 사용자 ID로 CustomUserDetails 로드
     */
    @Transactional(readOnly = true)
    public CustomUserDetails loadUserById(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("User not found"));
        return new CustomUserDetails(user);
    }

    /**
     * 현재 사용자 정보 조회
     */
    @Transactional(readOnly = true)
    public UserResponseDto getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("User not found"));
        return UserResponseDto.from(user);
    }

    /**
     * 이메일 회원가입
     */
    @Transactional
    public AuthResponseDto registerWithEmail(EmailRegisterRequestDto request) {
        // 필수 약관 동의 확인
        if (!Boolean.TRUE.equals(request.getTermsAgreed()) || 
            !Boolean.TRUE.equals(request.getPrivacyAgreed())) {
            throw new AuthException("필수 약관에 동의해주세요");
        }

        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("이미 사용 중인 이메일입니다", "CONFLICT");
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 사용자 생성
        User newUser = User.builder()
            .email(request.getEmail())
            .password(encodedPassword)
            .nickname(extractNicknameFromEmail(request.getEmail()))
            .provider(AuthProvider.EMAIL)
            .providerId(request.getEmail())  // 이메일 로그인의 경우 이메일을 providerId로 사용
            .role(User.Role.USER)
            .termsAgreed(request.getTermsAgreed())
            .privacyAgreed(request.getPrivacyAgreed())
            .marketingAgreed(request.getMarketingAgreed())
            .build();

        User savedUser = userRepository.save(newUser);
        log.info("New email user registered: userId={}, email={}", savedUser.getId(), savedUser.getEmail());

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(savedUser);
        String refreshToken = jwtTokenProvider.createRefreshToken(savedUser);

        // Refresh Token 저장
        saveRefreshToken(savedUser, refreshToken, request.getDeviceInfo());

        return AuthResponseDto.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtTokenProvider.getAccessTokenValidity() / 1000)
            .user(UserResponseDto.from(savedUser))
            .build();
    }

    /**
     * 이메일 로그인
     */
    @Transactional
    public AuthResponseDto loginWithEmail(EmailLoginRequestDto request) {
        // 이메일로 사용자 조회
        User user = userRepository.findByEmailAndProvider(request.getEmail(), AuthProvider.EMAIL)
            .orElseThrow(() -> new AuthException("이메일 또는 비밀번호가 올바르지 않습니다", "UNAUTHORIZED"));

        // 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException("이메일 또는 비밀번호가 올바르지 않습니다", "UNAUTHORIZED");
        }

        // 마지막 로그인 시간 업데이트
        user.updateLastLogin();

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);

        // Refresh Token 저장
        saveRefreshToken(user, refreshToken, request.getDeviceInfo());

        log.info("Email user logged in: userId={}", user.getId());

        return AuthResponseDto.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtTokenProvider.getAccessTokenValidity() / 1000)
            .user(UserResponseDto.from(user))
            .build();
    }

    /**
     * 프로필 수정
     */
    @Transactional
    public UserResponseDto updateProfile(Long userId, ProfileUpdateRequestDto request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("User not found"));

        user.updateNickname(request.getNickname());
        
        log.info("User profile updated: userId={}", userId);
        
        return UserResponseDto.from(user);
    }

    /**
     * 이메일에서 닉네임 추출 (@ 앞부분)
     */
    private String extractNicknameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "사용자";
        }
        return email.substring(0, email.indexOf("@"));
    }

    private User createNewUser(SocialUserInfo socialUserInfo, AuthProvider provider) {
        User newUser = User.builder()
            .email(socialUserInfo.getEmail())
            .nickname(socialUserInfo.getNickname())
            .profileImageUrl(socialUserInfo.getProfileImageUrl())
            .provider(provider)
            .providerId(socialUserInfo.getId())
            .role(User.Role.USER)
            .build();

        User savedUser = userRepository.save(newUser);
        log.info("New user created: userId={}, provider={}", savedUser.getId(), provider);
        return savedUser;
    }

    private void saveRefreshToken(User user, String token, String deviceInfo) {
        LocalDateTime expiresAt = LocalDateTime.ofInstant(
            jwtTokenProvider.getRefreshTokenExpiryDate().toInstant(),
            ZoneId.systemDefault()
        );

        // 새로운 토큰 패밀리 생성 (로그인 세션 식별용)
        String tokenFamily = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
            .user(user)
            .token(token)
            .deviceInfo(deviceInfo)
            .expiresAt(expiresAt)
            .tokenFamily(tokenFamily)
            .build();

        refreshTokenRepository.save(refreshToken);
    }
}
