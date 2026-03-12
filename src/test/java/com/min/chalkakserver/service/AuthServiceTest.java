package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.auth.*;
import com.min.chalkakserver.entity.RefreshToken;
import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.entity.User.AuthProvider;
import com.min.chalkakserver.exception.AuthException;
import com.min.chalkakserver.repository.CongestionReportRepository;
import com.min.chalkakserver.repository.FavoriteRepository;
import com.min.chalkakserver.repository.PhotoBoothReportRepository;
import com.min.chalkakserver.repository.RefreshTokenRepository;
import com.min.chalkakserver.repository.ReviewRepository;
import com.min.chalkakserver.repository.UserRepository;
import com.min.chalkakserver.security.CustomUserDetails;
import com.min.chalkakserver.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 테스트")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private FavoriteRepository favoriteRepository;
    @Mock private CongestionReportRepository congestionReportRepository;
    @Mock private PhotoBoothReportRepository photoBoothReportRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private SocialAuthService socialAuthService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@test.com")
                .nickname("tester")
                .provider(AuthProvider.EMAIL)
                .providerId("test@test.com")
                .role(User.Role.USER)
                .password("encodedPassword")
                .termsAgreed(true)
                .privacyAgreed(true)
                .build();
        setEntityId(testUser, 1L);
    }

    // ==================== socialLogin ====================

    @Nested
    @DisplayName("소셜 로그인 테스트")
    class SocialLoginTest {

        private SocialLoginRequestDto buildRequest(String provider) {
            return SocialLoginRequestDto.builder()
                    .provider(provider)
                    .accessToken("social-access-token")
                    .deviceInfo("iOS")
                    .build();
        }

        private void mockTokenCreation() {
            given(jwtTokenProvider.createAccessToken(any(User.class))).willReturn("access-token");
            given(jwtTokenProvider.createRefreshToken(any(User.class))).willReturn("refresh-token");
            given(jwtTokenProvider.getAccessTokenValidity()).willReturn(3600000L);
            given(jwtTokenProvider.getRefreshTokenExpiryDate())
                    .willReturn(new Date(System.currentTimeMillis() + 14 * 24 * 60 * 60 * 1000L));
        }

        @Test
        @DisplayName("신규 소셜 유저를 생성하고 토큰을 반환한다")
        void socialLogin_NewUser() {
            // given
            SocialLoginRequestDto request = buildRequest("kakao");
            SocialUserInfo socialUserInfo = SocialUserInfo.builder()
                    .id("kakao-123")
                    .email("kakao@test.com")
                    .nickname("카카오유저")
                    .profileImageUrl("https://img.test.com/profile.jpg")
                    .build();

            given(socialAuthService.getSocialUserInfo("kakao", "social-access-token"))
                    .willReturn(socialUserInfo);
            given(userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, "kakao-123"))
                    .willReturn(Optional.empty());

            User savedUser = User.builder()
                    .email("kakao@test.com")
                    .nickname("카카오유저")
                    .provider(AuthProvider.KAKAO)
                    .providerId("kakao-123")
                    .role(User.Role.USER)
                    .build();
            setEntityId(savedUser, 2L);
            given(userRepository.save(any(User.class))).willReturn(savedUser);
            mockTokenCreation();

            // when
            AuthResponseDto result = authService.socialLogin(request);

            // then
            assertThat(result.getAccessToken()).isEqualTo("access-token");
            assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(result.getTokenType()).isEqualTo("Bearer");
            assertThat(result.getExpiresIn()).isEqualTo(3600L);
            assertThat(result.getUser().getEmail()).isEqualTo("kakao@test.com");
            verify(userRepository).save(any(User.class));
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("기존 소셜 유저는 프로필을 업데이트하고 토큰을 반환한다")
        void socialLogin_ExistingUser() {
            // given
            SocialLoginRequestDto request = buildRequest("kakao");
            SocialUserInfo socialUserInfo = SocialUserInfo.builder()
                    .id("kakao-123")
                    .email("kakao@test.com")
                    .nickname("업데이트된닉네임")
                    .profileImageUrl("https://img.test.com/new-profile.jpg")
                    .build();

            User existingUser = User.builder()
                    .email("kakao@test.com")
                    .nickname("기존닉네임")
                    .provider(AuthProvider.KAKAO)
                    .providerId("kakao-123")
                    .role(User.Role.USER)
                    .build();
            setEntityId(existingUser, 3L);

            given(socialAuthService.getSocialUserInfo("kakao", "social-access-token"))
                    .willReturn(socialUserInfo);
            given(userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, "kakao-123"))
                    .willReturn(Optional.of(existingUser));
            mockTokenCreation();

            // when
            AuthResponseDto result = authService.socialLogin(request);

            // then
            assertThat(result.getAccessToken()).isEqualTo("access-token");
            assertThat(result.getUser().getNickname()).isEqualTo("업데이트된닉네임");
            verify(userRepository, never()).save(any(User.class));
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }
    }

    // ==================== refreshToken ====================

    @Nested
    @DisplayName("토큰 갱신 테스트")
    class RefreshTokenTest {

        private RefreshTokenRequestDto buildRequest(String token) {
            return RefreshTokenRequestDto.builder()
                    .refreshToken(token)
                    .build();
        }

        @Test
        @DisplayName("유효한 refresh token으로 새 토큰을 발급한다 (Token Rotation)")
        void refreshToken_Success() {
            // given
            String oldTokenStr = "old-refresh-token";
            RefreshToken storedToken = RefreshToken.builder()
                    .user(testUser)
                    .token(oldTokenStr)
                    .deviceInfo("iOS")
                    .expiresAt(LocalDateTime.now().plusDays(14))
                    .tokenFamily("family-uuid-1")
                    .build();

            given(jwtTokenProvider.validateToken(oldTokenStr)).willReturn(true);
            given(refreshTokenRepository.findByToken(oldTokenStr)).willReturn(Optional.of(storedToken));
            given(jwtTokenProvider.createAccessToken(testUser)).willReturn("new-access-token");
            given(jwtTokenProvider.createRefreshToken(testUser)).willReturn("new-refresh-token");
            given(jwtTokenProvider.getAccessTokenValidity()).willReturn(3600000L);
            given(jwtTokenProvider.getRefreshTokenExpiryDate())
                    .willReturn(new Date(System.currentTimeMillis() + 14 * 24 * 60 * 60 * 1000L));

            // when
            AuthResponseDto result = authService.refreshToken(buildRequest(oldTokenStr));

            // then
            assertThat(result.getAccessToken()).isEqualTo("new-access-token");
            assertThat(result.getRefreshToken()).isEqualTo("new-refresh-token");
            assertThat(storedToken.isUsed()).isTrue();

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());
            assertThat(captor.getValue().getTokenFamily()).isEqualTo("family-uuid-1");
        }

        @Test
        @DisplayName("이미 사용된 토큰 재사용 시 tokenFamily 전체 삭제 및 예외 발생 (재사용 감지)")
        void refreshToken_ReuseDetection() {
            // given
            String usedTokenStr = "used-refresh-token";
            RefreshToken usedToken = RefreshToken.builder()
                    .user(testUser)
                    .token(usedTokenStr)
                    .deviceInfo("iOS")
                    .expiresAt(LocalDateTime.now().plusDays(14))
                    .tokenFamily("family-uuid-2")
                    .build();
            usedToken.markAsUsed();

            given(jwtTokenProvider.validateToken(usedTokenStr)).willReturn(true);
            given(refreshTokenRepository.findByToken(usedTokenStr)).willReturn(Optional.of(usedToken));

            // when & then
            assertThatThrownBy(() -> authService.refreshToken(buildRequest(usedTokenStr)))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("revoked");

            verify(refreshTokenRepository).deleteAllByTokenFamily("family-uuid-2");
        }

        @Test
        @DisplayName("JWT 검증 실패 시 AuthException 발생")
        void refreshToken_InvalidToken() {
            // given
            given(jwtTokenProvider.validateToken("invalid-token")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.refreshToken(buildRequest("invalid-token")))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("Invalid refresh token");

            verify(refreshTokenRepository, never()).findByToken(any());
        }

        @Test
        @DisplayName("만료된 토큰은 삭제 후 AuthException 발생")
        void refreshToken_Expired() {
            // given
            String expiredTokenStr = "expired-refresh-token";
            RefreshToken expiredToken = RefreshToken.builder()
                    .user(testUser)
                    .token(expiredTokenStr)
                    .deviceInfo("iOS")
                    .expiresAt(LocalDateTime.now().plusDays(1))
                    .tokenFamily("family-uuid-3")
                    .build();
            // 만료 시간을 과거로 설정
            setField(expiredToken, "expiresAt", LocalDateTime.now().minusDays(1));

            given(jwtTokenProvider.validateToken(expiredTokenStr)).willReturn(true);
            given(refreshTokenRepository.findByToken(expiredTokenStr)).willReturn(Optional.of(expiredToken));

            // when & then
            assertThatThrownBy(() -> authService.refreshToken(buildRequest(expiredTokenStr)))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("expired");

            verify(refreshTokenRepository).delete(expiredToken);
        }

        @Test
        @DisplayName("DB에 존재하지 않는 token 조회 시 AuthException 발생")
        void refreshToken_NotFound() {
            // given
            String unknownToken = "unknown-token";
            given(jwtTokenProvider.validateToken(unknownToken)).willReturn(true);
            given(refreshTokenRepository.findByToken(unknownToken)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.refreshToken(buildRequest(unknownToken)))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("not found");
        }
    }

    // ==================== logout ====================

    @Nested
    @DisplayName("로그아웃 테스트")
    class LogoutTest {

        @Test
        @DisplayName("refresh token을 삭제하여 로그아웃한다")
        void logout_DeletesRefreshToken() {
            // given
            String refreshToken = "some-refresh-token";

            // when
            authService.logout(refreshToken);

            // then
            verify(refreshTokenRepository).deleteByToken(refreshToken);
        }
    }

    // ==================== logoutAll ====================

    @Nested
    @DisplayName("전체 기기 로그아웃 테스트")
    class LogoutAllTest {

        @Test
        @DisplayName("사용자의 모든 refresh token을 삭제한다")
        void logoutAll_DeletesAllTokens() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            // when
            authService.logoutAll(1L);

            // then
            verify(refreshTokenRepository).deleteAllByUser(testUser);
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 전체 로그아웃 시 AuthException 발생")
        void logoutAll_UserNotFound() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.logoutAll(999L))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("User not found");

            verify(refreshTokenRepository, never()).deleteAllByUser(any());
        }
    }

    // ==================== deleteAccount ====================

    @Nested
    @DisplayName("회원 탈퇴 테스트")
    class DeleteAccountTest {

        @Test
        @DisplayName("회원 탈퇴 시 연관 데이터를 순서대로 삭제한다")
        void deleteAccount_CascadingDelete() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            // when
            authService.deleteAccount(1L);

            // then - 순서 검증
            var inOrder = inOrder(reviewRepository, favoriteRepository,
                    congestionReportRepository, photoBoothReportRepository,
                    refreshTokenRepository, userRepository);
            inOrder.verify(reviewRepository).deleteAllByUser(testUser);
            inOrder.verify(favoriteRepository).deleteAllByUser(testUser);
            inOrder.verify(congestionReportRepository).deleteAllByUser(testUser);
            inOrder.verify(photoBoothReportRepository).deleteAllByUser(testUser);
            inOrder.verify(refreshTokenRepository).deleteAllByUser(testUser);
            inOrder.verify(userRepository).delete(testUser);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 탈퇴 시 AuthException 발생")
        void deleteAccount_UserNotFound() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.deleteAccount(999L))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("User not found");

            verify(userRepository, never()).delete(any());
        }
    }

    // ==================== registerWithEmail ====================

    @Nested
    @DisplayName("이메일 회원가입 테스트")
    class RegisterWithEmailTest {

        private EmailRegisterRequestDto buildRequest(String email, Boolean termsAgreed, Boolean privacyAgreed) {
            return EmailRegisterRequestDto.builder()
                    .email(email)
                    .password("password1234")
                    .termsAgreed(termsAgreed)
                    .privacyAgreed(privacyAgreed)
                    .marketingAgreed(false)
                    .deviceInfo("Android")
                    .build();
        }

        @Test
        @DisplayName("이메일 회원가입 성공 시 토큰과 사용자 정보를 반환한다")
        void registerWithEmail_Success() {
            // given
            EmailRegisterRequestDto request = buildRequest("new@test.com", true, true);
            given(userRepository.existsByEmail("new@test.com")).willReturn(false);
            given(passwordEncoder.encode("password1234")).willReturn("encoded-password");

            User savedUser = User.builder()
                    .email("new@test.com")
                    .nickname("new")
                    .provider(AuthProvider.EMAIL)
                    .providerId("new@test.com")
                    .role(User.Role.USER)
                    .password("encoded-password")
                    .termsAgreed(true)
                    .privacyAgreed(true)
                    .build();
            setEntityId(savedUser, 10L);
            given(userRepository.save(any(User.class))).willReturn(savedUser);
            given(jwtTokenProvider.createAccessToken(savedUser)).willReturn("access-token");
            given(jwtTokenProvider.createRefreshToken(savedUser)).willReturn("refresh-token");
            given(jwtTokenProvider.getAccessTokenValidity()).willReturn(3600000L);
            given(jwtTokenProvider.getRefreshTokenExpiryDate())
                    .willReturn(new Date(System.currentTimeMillis() + 14 * 24 * 60 * 60 * 1000L));

            // when
            AuthResponseDto result = authService.registerWithEmail(request);

            // then
            assertThat(result.getAccessToken()).isEqualTo("access-token");
            assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(result.getUser().getEmail()).isEqualTo("new@test.com");
            verify(passwordEncoder).encode("password1234");
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("이미 사용 중인 이메일로 가입 시 CONFLICT AuthException 발생")
        void registerWithEmail_DuplicateEmail() {
            // given
            EmailRegisterRequestDto request = buildRequest("dup@test.com", true, true);
            given(userRepository.existsByEmail("dup@test.com")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.registerWithEmail(request))
                    .isInstanceOf(AuthException.class)
                    .satisfies(ex -> assertThat(((AuthException) ex).getCode()).isEqualTo("CONFLICT"));

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("이용약관 미동의 시 AuthException 발생")
        void registerWithEmail_TermsNotAgreed() {
            // given
            EmailRegisterRequestDto request = buildRequest("test2@test.com", false, true);

            // when & then
            assertThatThrownBy(() -> authService.registerWithEmail(request))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("약관");

            verify(userRepository, never()).existsByEmail(any());
        }

        @Test
        @DisplayName("개인정보처리방침 미동의 시 AuthException 발생")
        void registerWithEmail_PrivacyNotAgreed() {
            // given
            EmailRegisterRequestDto request = buildRequest("test3@test.com", true, false);

            // when & then
            assertThatThrownBy(() -> authService.registerWithEmail(request))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("약관");

            verify(userRepository, never()).existsByEmail(any());
        }
    }

    // ==================== loginWithEmail ====================

    @Nested
    @DisplayName("이메일 로그인 테스트")
    class LoginWithEmailTest {

        private EmailLoginRequestDto buildRequest(String email, String password) {
            return EmailLoginRequestDto.builder()
                    .email(email)
                    .password(password)
                    .deviceInfo("iOS")
                    .build();
        }

        @Test
        @DisplayName("올바른 이메일/비밀번호로 로그인 성공")
        void loginWithEmail_Success() {
            // given
            EmailLoginRequestDto request = buildRequest("test@test.com", "password1234");
            given(userRepository.findByEmailAndProvider("test@test.com", AuthProvider.EMAIL))
                    .willReturn(Optional.of(testUser));
            given(passwordEncoder.matches("password1234", "encodedPassword")).willReturn(true);
            given(jwtTokenProvider.createAccessToken(testUser)).willReturn("access-token");
            given(jwtTokenProvider.createRefreshToken(testUser)).willReturn("refresh-token");
            given(jwtTokenProvider.getAccessTokenValidity()).willReturn(3600000L);
            given(jwtTokenProvider.getRefreshTokenExpiryDate())
                    .willReturn(new Date(System.currentTimeMillis() + 14 * 24 * 60 * 60 * 1000L));

            // when
            AuthResponseDto result = authService.loginWithEmail(request);

            // then
            assertThat(result.getAccessToken()).isEqualTo("access-token");
            assertThat(result.getUser().getEmail()).isEqualTo("test@test.com");
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("틀린 비밀번호로 로그인 시 UNAUTHORIZED AuthException 발생")
        void loginWithEmail_WrongPassword() {
            // given
            EmailLoginRequestDto request = buildRequest("test@test.com", "wrong-password");
            given(userRepository.findByEmailAndProvider("test@test.com", AuthProvider.EMAIL))
                    .willReturn(Optional.of(testUser));
            given(passwordEncoder.matches("wrong-password", "encodedPassword")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.loginWithEmail(request))
                    .isInstanceOf(AuthException.class)
                    .satisfies(ex -> assertThat(((AuthException) ex).getCode()).isEqualTo("UNAUTHORIZED"));

            verify(jwtTokenProvider, never()).createAccessToken(any());
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 로그인 시 UNAUTHORIZED AuthException 발생")
        void loginWithEmail_UserNotFound() {
            // given
            EmailLoginRequestDto request = buildRequest("notfound@test.com", "password1234");
            given(userRepository.findByEmailAndProvider("notfound@test.com", AuthProvider.EMAIL))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.loginWithEmail(request))
                    .isInstanceOf(AuthException.class)
                    .satisfies(ex -> assertThat(((AuthException) ex).getCode()).isEqualTo("UNAUTHORIZED"));

            verify(passwordEncoder, never()).matches(any(), any());
        }
    }

    // ==================== updateProfile ====================

    @Nested
    @DisplayName("프로필 수정 테스트")
    class UpdateProfileTest {

        @Test
        @DisplayName("닉네임 수정 성공 시 UserResponseDto를 반환한다")
        void updateProfile_Success() {
            // given
            ProfileUpdateRequestDto request = ProfileUpdateRequestDto.builder()
                    .nickname("새닉네임")
                    .build();
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            // when
            UserResponseDto result = authService.updateProfile(1L, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(testUser.getNickname()).isEqualTo("새닉네임");
        }

        @Test
        @DisplayName("존재하지 않는 사용자 프로필 수정 시 AuthException 발생")
        void updateProfile_UserNotFound() {
            // given
            ProfileUpdateRequestDto request = ProfileUpdateRequestDto.builder()
                    .nickname("새닉네임")
                    .build();
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.updateProfile(999L, request))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("User not found");
        }
    }

    // ==================== loadUserById ====================

    @Nested
    @DisplayName("loadUserById 테스트")
    class LoadUserByIdTest {

        @Test
        @DisplayName("사용자 ID로 CustomUserDetails를 반환한다")
        void loadUserById_Success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            // when
            CustomUserDetails result = authService.loadUserById(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("test@test.com");
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회 시 AuthException 발생")
        void loadUserById_NotFound() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.loadUserById(999L))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("User not found");
        }
    }

    // ==================== getCurrentUser ====================

    @Nested
    @DisplayName("getCurrentUser 테스트")
    class GetCurrentUserTest {

        @Test
        @DisplayName("사용자 ID로 UserResponseDto를 반환한다")
        void getCurrentUser_Success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            // when
            UserResponseDto result = authService.getCurrentUser(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("test@test.com");
            assertThat(result.getNickname()).isEqualTo("tester");
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회 시 AuthException 발생")
        void getCurrentUser_NotFound() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.getCurrentUser(999L))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("User not found");
        }
    }

    // ==================== helpers ====================

    private void setEntityId(Object entity, Long id) {
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
